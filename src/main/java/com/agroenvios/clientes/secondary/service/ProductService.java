package com.agroenvios.clientes.secondary.service;

import com.agroenvios.clientes.primary.service.MinioService;
import com.agroenvios.clientes.secondary.dto.CategoryOptionDTO;
import com.agroenvios.clientes.secondary.dto.ProductPageDTO;
import com.agroenvios.clientes.secondary.dto.ProductResponseDTO;
import com.agroenvios.clientes.secondary.dto.ProductUnitDTO;
import com.agroenvios.clientes.secondary.model.Product;
import com.agroenvios.clientes.secondary.repository.CategoryProductRepository;
import com.agroenvios.clientes.secondary.repository.ProductRepository;
import com.agroenvios.clientes.secondary.repository.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryProductRepository categoryProductRepository;
    private final ProductUnitRepository productUnitRepository;
    private final MinioService minioService;

    @Value("${aws.s3.proveedores-bucket:agroenvios-files}")
    private String proveedoresBucket;

    private ProductResponseDTO toDTO(Product product) {
        String imageUrl = null;
        if (product.getImageKey() != null && !product.getImageKey().isBlank()) {
            imageUrl = minioService.generatePresignedUrl(product.getImageKey(), proveedoresBucket);
        }

        // Si el producto aún no tiene miniatura (subida antes de este feature), usamos
        // la imagen completa como respaldo para no dejar la miniatura vacía.
        String thumbnailUrl = imageUrl;
        if (product.getThumbnailKey() != null && !product.getThumbnailKey().isBlank()) {
            thumbnailUrl = minioService.generatePresignedUrl(product.getThumbnailKey(), proveedoresBucket);
        }

        String comercioNombre = null;
        String comercioLogoUrl = null;
        if (product.getTradeShop() != null) {
            comercioNombre = product.getTradeShop().getNombreNegocio();
            String logoKey = product.getTradeShop().getImageKey();
            if (logoKey != null && !logoKey.isBlank()) {
                comercioLogoUrl = minioService.generatePresignedUrl(logoKey, proveedoresBucket);
            }
        }

        return ProductResponseDTO.from(product, imageUrl, thumbnailUrl, comercioNombre, comercioLogoUrl);
    }

    public List<ProductResponseDTO> getAll() {
        return productRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<ProductResponseDTO> getAvailable() {
        return productRepository.findInStock().stream().map(this::toDTO).toList();
    }

    public ProductResponseDTO getById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return toDTO(product);
    }

    public List<ProductResponseDTO> getByCategory(Integer categoryId) {
        return productRepository.findInStockByCategoryId(categoryId)
                .stream().map(this::toDTO).toList();
    }

    public List<ProductResponseDTO> searchByName(String name) {
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(name)
                .stream().map(this::toDTO).toList();
    }

    /**
     * Trae productos disponibles (activos, marcados como disponibles y con stock) en páginas
     * para scroll infinito, con filtro opcional de categoría y búsqueda por nombre resueltos
     * del lado del servidor.
     */
    public ProductPageDTO getAvailablePaged(Integer categoryId, String search, int page, int size, Long seed) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();

        // Orden intercalado: se mezclan los productos de cada comercio y se reparten en
        // rotación (uno de cada comercio, luego otro de cada uno...) para que el catálogo
        // no muestre todos los de un comercio seguidos. La semilla mantiene estable el
        // orden entre páginas de una misma sesión y lo cambia al refrescar.
        List<Object[]> rows = candidatosCatalogo(categoryId);
        List<String> tokens = normalizedSearch == null ? List.of() : tokenize(normalizedSearch);
        java.util.Map<Integer, Integer> rank = new java.util.HashMap<>();
        if (!tokens.isEmpty()) {
            rows = rows.stream().filter(r -> {
                int score = searchScore(r, tokens);
                if (score < 0) return false;
                rank.put(((Number) r[0]).intValue(), score);
                return true;
            }).toList();
        }
        List<Integer> ordered = interleaveByShop(rows, seed != null ? seed : 0L);
        // Con búsqueda, lo que mejor coincide va primero (sort estable: conserva el intercalado)
        if (!tokens.isEmpty()) ordered = ordered.stream().sorted(java.util.Comparator.comparingInt(rank::get)).toList();

        int from = (int) Math.min((long) page * size, ordered.size());
        int to = Math.min(from + size, ordered.size());
        List<Integer> pageIds = ordered.subList(from, to);

        java.util.Map<Integer, Product> byId = new java.util.HashMap<>();
        productRepository.findAllById(pageIds).forEach(p -> byId.put(p.getId(), p));
        List<ProductResponseDTO> items = pageIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                // La lista de candidatos puede tener unos segundos de antigüedad: lo que se agotó
                // o se desactivó en ese lapso no se muestra.
                .filter(p -> Boolean.TRUE.equals(p.getActive()) && Boolean.TRUE.equals(p.getAvailable())
                        && p.getStockAvailable() != null && p.getStockAvailable() > 0)
                .map(this::toDTO)
                .toList();
        return ProductPageDTO.builder()
                .items(items)
                .hasMore(to < ordered.size())
                .page(page)
                .build();
    }

    private static final long CANDIDATOS_TTL_MS = 15_000;
    private record Candidatos(long expiraEn, List<Object[]> filas) {}
    private final java.util.concurrent.ConcurrentHashMap<Integer, Candidatos> candidatosCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Productos visibles del catálogo (id, comercio, nombre, comercio, categoría) por categoría,
     * con una copia de pocos segundos en memoria: cada página y cada cambio de filtro de la app
     * pedían la lista completa a la base de datos.
     */
    private List<Object[]> candidatosCatalogo(Integer categoryId) {
        int clave = categoryId == null ? -1 : categoryId;
        long ahora = System.currentTimeMillis();
        Candidatos guardado = candidatosCache.get(clave);
        if (guardado != null && guardado.expiraEn() > ahora) return guardado.filas();
        List<Object[]> filas = productRepository.findAvailableForSearch(categoryId);
        candidatosCache.put(clave, new Candidatos(ahora + CANDIDATOS_TTL_MS, filas));
        return filas;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private static List<String> tokenize(String search) {
        return java.util.Arrays.stream(normalize(search).split("\\s+")).filter(t -> !t.isEmpty()).toList();
    }

    /**
     * -1 si no coincide. Todas las palabras deben aparecer en nombre, comercio o categoría
     * (sin acentos ni mayúsculas). Menor es mejor: 0 empieza con la palabra, 1 una palabra del
     * nombre empieza con ella, 2 está dentro del nombre, 3 solo coincide por comercio/categoría.
     */
    private static int searchScore(Object[] row, List<String> tokens) {
        String name = normalize((String) row[2]);
        String other = normalize((String) row[3]) + " " + normalize((String) row[4]);
        int worst = 0;
        for (String token : tokens) {
            int score;
            if (name.startsWith(token)) score = 0;
            else if (name.contains(" " + token)) score = 1;
            else if (name.contains(token)) score = 2;
            else if (other.contains(token)) score = 3;
            else return -1;
            worst = Math.max(worst, score);
        }
        return worst;
    }

    private static List<Integer> interleaveByShop(List<Object[]> rows, long seed) {
        java.util.Random random = new java.util.Random(seed);
        java.util.Map<Long, List<Integer>> byShop = new java.util.TreeMap<>();
        for (Object[] row : rows) {
            Long shopId = row[1] == null ? -1L : ((Number) row[1]).longValue();
            byShop.computeIfAbsent(shopId, k -> new java.util.ArrayList<>()).add(((Number) row[0]).intValue());
        }
        List<List<Integer>> groups = new java.util.ArrayList<>(byShop.values());
        for (List<Integer> g : groups) {
            g.sort(Integer::compare); // base determinista antes de mezclar con la semilla
            java.util.Collections.shuffle(g, random);
        }
        java.util.Collections.shuffle(groups, random);

        List<Integer> result = new java.util.ArrayList<>(rows.size());
        for (int round = 0; result.size() < rows.size(); round++) {
            for (List<Integer> g : groups) {
                if (round < g.size()) result.add(g.get(round));
            }
        }
        return result;
    }

    /**
     * Unidades activas con su incremento, administradas desde el panel admin. Si la tabla
     * todavía no existe (backendAgroBasicos la crea al desplegar) se devuelve vacío y la
     * app usa sus incrementos por defecto.
     */
    public List<ProductUnitDTO> getActiveUnits() {
        try {
            return productUnitRepository.findByActiveTrue().stream()
                    .map(u -> new ProductUnitDTO(u.getCode(), u.getName(), u.getStep(), u.getMinQuantity()))
                    .toList();
        } catch (Exception e) {
            log.warn("No se pudieron leer las unidades de producto: {}", e.getMessage());
            return List.of();
        }
    }

    public List<CategoryOptionDTO> getAvailableCategories() {
        return productRepository.countAvailableByCategory().stream()
                .map(r -> new CategoryOptionDTO(((Number) r[0]).intValue(), (String) r[1], ((Number) r[2]).longValue()))
                .toList();
    }
}
