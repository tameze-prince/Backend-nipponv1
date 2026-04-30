package prod.nipponhubv1.nipponhubv1.Services;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.BulkProductRequest;
import prod.nipponhubv1.nipponhubv1.Dto.FlashSaleInfo;
import prod.nipponhubv1.nipponhubv1.Dto.ProductRequest;
import prod.nipponhubv1.nipponhubv1.Dto.ProductResponse;
import prod.nipponhubv1.nipponhubv1.Dto.VariantRequest;
import prod.nipponhubv1.nipponhubv1.Dto.VariantResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.ProductMapper;
import prod.nipponhubv1.nipponhubv1.Mappers.VariantMapper;
import prod.nipponhubv1.nipponhubv1.Models.Category;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.Franchise;
import prod.nipponhubv1.nipponhubv1.Models.Product;
import prod.nipponhubv1.nipponhubv1.Models.ProductImage;
import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;
import prod.nipponhubv1.nipponhubv1.Models.Stock;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.CategoryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.FlashSaleRepository;
import prod.nipponhubv1.nipponhubv1.Repository.FranchiseRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductImageRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductVariantRepository;
import prod.nipponhubv1.nipponhubv1.Repository.StockRepository;

/**
 * Gestion du catalogue produits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository         productRepo;
    private final ProductVariantRepository  variantRepo;
    private final ProductImageRepository    imageRepo;
    private final CategoryRepository        categoryRepo;
    private final CountryRepository         countryRepo;
    private final FranchiseRepository       franchiseRepo;
    private final FlashSaleRepository       flashSaleRepo;
    private final StockRepository           stockRepo;
    private final CloudinaryService         cloudinaryService;
    private final ProductMapper             productMapper;
    private final VariantMapper             variantMapper;

    @Transactional
    public Page<ProductResponse> getCatalogue(Long categoryId,
                                              Long franchiseId,
                                              String keyword,
                                              Long countryId,
                                              Long cityId,
                                              OurUser requester,
                                              Pageable pageable) {
        Long effectiveCountryId = resolveAccessibleCountryId(countryId, cityId, requester);
        Page<Product> page;

        if (keyword != null && !keyword.isBlank()) {
            page = productRepo.searchByKeyword(keyword.trim(), pageable);
        } else {
            page = productRepo.findActiveFiltered(categoryId, franchiseId, pageable);
        }

        return page.map(product -> buildProductResponse(product, effectiveCountryId));
    }

    @Transactional
    public ProductResponse getBySlug(String slug, Long countryId, Long cityId, OurUser requester) {
        Long effectiveCountryId = resolveAccessibleCountryId(countryId, cityId, requester);
        Product product = productRepo.findBySlugWithAssociations(slug)
            .filter(Product::isActive)
            .orElseThrow(() -> OtakuException.notFound("Produit (slug=" + slug + ")", 0L));
        return buildProductResponse(product, effectiveCountryId);
    }

    @Transactional
    public List<ProductResponse> getFlashSaleProducts(Long countryId) {
        return productRepo.findActiveFlashSaleProducts(LocalDateTime.now())
            .stream()
            .map(product -> buildProductResponse(product, countryId))
            .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req,
                                         List<MultipartFile> images,
                                         OurUser requester) throws IOException {
        return createSingleProduct(req, images, requester);
    }

    @Transactional
    public List<ProductResponse> createProducts(BulkProductRequest req,
                                                List<MultipartFile> images,
                                                OurUser requester) throws IOException {
        List<ProductResponse> responses = new ArrayList<>();
        List<MultipartFile> safeImages = images != null ? images : List.of();
        int imageCursor = 0;

        for (ProductRequest productRequest : req.getProducts()) {
            int imageCount = productRequest.getImageCount() != null
                ? Math.max(0, productRequest.getImageCount())
                : 0;
            int endIndex = Math.min(imageCursor + imageCount, safeImages.size());
            List<MultipartFile> productImages = safeImages.subList(imageCursor, endIndex);

            responses.add(createSingleProduct(productRequest, productImages, requester));
            imageCursor = endIndex;
        }

        return responses;
    }

    @Transactional
    public ProductResponse updateProduct(Long id,
                                         ProductRequest req,
                                         List<MultipartFile> newImages,
                                         OurUser requester) throws IOException {
        Product product = productRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Produit", id));

        // Vérifier les permissions
        checkProductOwnership(product, requester);

        if (req.getCategoryId() != null) {
            product.setCategory(
                categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> OtakuException.notFound("Catégorie", req.getCategoryId()))
            );
        }
        if (req.getFranchiseId() != null) {
            product.setFranchise(
                franchiseRepo.findById(req.getFranchiseId())
                    .orElseThrow(() -> OtakuException.notFound("Franchise", req.getFranchiseId()))
            );
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            product.setName(req.getName());
            product.setSlug(generateUniqueSlug(req.getName()));
        }
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getBasePrice() != null) product.setBasePrice(req.getBasePrice());
        if (req.getPurchasePrice() != null) product.setPurchasePrice(req.getPurchasePrice());

        if (newImages != null && !newImages.isEmpty()) {
            saveProductImages(product, newImages);
        }

        return buildProductResponse(productRepo.save(product), null);
    }

    @Transactional
    public void deleteProductImage(Long imageId) {
        ProductImage image = imageRepo.findById(imageId)
            .orElseThrow(() -> OtakuException.notFound("Image", imageId));

        cloudinaryService.delete(extractPublicId(image.getUrl()));
        imageRepo.delete(image);
        log.info("Image supprimée - id={}", imageId);
    }

    @Transactional
    public void deactivateProduct(Long id, OurUser requester) {
        Product product = productRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Produit", id));

        // Vérifier les permissions
        checkProductOwnership(product, requester);

        product.setActive(false);
        productRepo.save(product);
        log.info("Produit désactivé - id={} by={}", id, requester.getEmail());
    }

    @Transactional
    public VariantResponse addVariant(Long productId, VariantRequest req, OurUser requester) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> OtakuException.notFound("Produit", productId));

        // Vérifier les permissions
        checkProductOwnership(product, requester);

        ProductVariant variant = variantMapper.toEntity(req);
        variant.setProduct(product);
        ProductVariant saved = variantRepo.save(variant);

        log.info("Variante ajoutée - productId={} variantId={}", productId, saved.getId());
        return variantMapper.toResponse(saved);
    }

    @Transactional
    public void deleteVariant(Long variantId, OurUser requester) {
        ProductVariant variant = variantRepo.findById(variantId)
            .orElseThrow(() -> OtakuException.notFound("Variante", variantId));

        // Vérifier les permissions
        checkProductOwnership(variant.getProduct(), requester);

        variant.setActive(false);
        variantRepo.save(variant);
        log.info("Variante désactivée - variantId={}", variantId);
    }

    // ── Helper: Vérifier la propriété d'un produit ────────────────────────────

    private void checkProductOwnership(Product product, OurUser requester) {
        if (requester.getRole() == Role.ADMIN) {
            return; // Admin peut tout faire
        }

        if (requester.getRole() == Role.OWNER) {
            if (product.getCreatedByUser() == null
                || !product.getCreatedByUser().getId().equals(requester.getId())) {
                throw OtakuException.forbidden(
                    "Vous n'avez pas la permission de modifier ce produit."
                );
            }
            return;
        }

        throw OtakuException.forbidden(
            "Seul un ADMIN ou un OWNER peut gérer les produits."
        );
    }

    public ProductResponse buildProductResponse(Product product, Long countryId) {
        ProductResponse response = productMapper.toResponse(product);

        flashSaleRepo.findActiveByProductId(product.getId(), LocalDateTime.now())
            .ifPresent(flashSale -> {
                BigDecimal pct = flashSale.getDiscountPct().divide(BigDecimal.valueOf(100));
                BigDecimal discount = product.getBasePrice().multiply(pct);
                BigDecimal discountedPrice = product.getBasePrice().subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);

                response.setActiveFlashSale(FlashSaleInfo.builder()
                    .discountPct(flashSale.getDiscountPct())
                    .discountedPrice(discountedPrice)
                    .endsAt(flashSale.getEndsAt())
                    .build());
            });

        List<VariantResponse> variants = (product.getVariants() != null ? product.getVariants() : List.<ProductVariant>of()).stream()
            .filter(ProductVariant::isActive)
            .map(variant -> {
                VariantResponse variantResponse = variantMapper.toResponse(variant);
                BigDecimal effectiveBase = response.getActiveFlashSale() != null
                    ? response.getActiveFlashSale().getDiscountedPrice()
                    : product.getBasePrice();
                variantResponse.setFinalPrice(effectiveBase.add(variant.getExtraPrice()));

                int qty = countryId != null
                    ? stockRepo.getQuantity(variant.getId(), countryId).orElse(0)
                    : stockRepo.getTotalQuantityByVariantId(variant.getId());
                variantResponse.setStockQuantity(qty);
                return variantResponse;
            })
            .toList();

        response.setVariants(variants);
        return response;
    }

    private ProductResponse createSingleProduct(ProductRequest req,
                                                List<MultipartFile> images,
                                                OurUser requester) throws IOException {
        Category category = categoryRepo.findById(req.getCategoryId())
            .orElseThrow(() -> OtakuException.notFound("Catégorie", req.getCategoryId()));

        Franchise franchise = null;
        if (req.getFranchiseId() != null) {
            franchise = franchiseRepo.findById(req.getFranchiseId())
                .orElseThrow(() -> OtakuException.notFound("Franchise", req.getFranchiseId()));
        }

        Product product = productMapper.toEntity(req);
        product.setCategory(category);
        product.setFranchise(franchise);
        product.setCreatedByUser(requester);  // Assigner le créateur
        product.setSlug(generateUniqueSlug(req.getName()));

        Product saved = productRepo.save(product);
        List<ProductVariant> createdVariants = saveProductVariants(saved, req);

        if (images != null && !images.isEmpty()) {
            saveProductImages(saved, images);
        }

        initializeStock(createdVariants, req);

        // Reload product from DB to ensure images are loaded
        Product reloaded = productRepo.findById(saved.getId()).orElseThrow();

        log.info("Produit créé - id={} slug={} by={}", reloaded.getId(), reloaded.getSlug(), requester.getEmail());
        return buildProductResponse(reloaded, null);
    }

    private List<ProductVariant> saveProductVariants(Product product, ProductRequest req) {
        List<ProductVariant> createdVariants = new ArrayList<>();

        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            for (VariantRequest variantRequest : req.getVariants()) {
                ProductVariant variant = variantMapper.toEntity(variantRequest);
                variant.setProduct(product);
                createdVariants.add(variantRepo.save(variant));
            }
            return createdVariants;
        }

        ProductVariant defaultVariant = ProductVariant.builder()
            .product(product)
            .label("Standard")
            .extraPrice(BigDecimal.ZERO)
            .active(true)
            .build();

        createdVariants.add(variantRepo.save(defaultVariant));
        return createdVariants;
    }

    private void initializeStock(List<ProductVariant> variants, ProductRequest req) {
        if (req.getInitialStock() == null || req.getInitialStock() <= 0 || variants.isEmpty()) {
            return;
        }

        Country country = resolveStockCountry(req.getStockCountryId());
        if (country == null) {
            log.warn("Aucun pays actif trouvé pour initialiser le stock de {}", req.getName());
            return;
        }

        ProductVariant targetVariant = variants.get(0);
        Stock stock = stockRepo.findByVariantIdAndCountryId(targetVariant.getId(), country.getId())
            .orElseGet(() -> Stock.builder()
                .variant(targetVariant)
                .country(country)
                .quantity(0)
                .build());

        stock.setQuantity(stock.getQuantity() + req.getInitialStock());
        stockRepo.save(stock);
    }

    private Country resolveStockCountry(Long stockCountryId) {
        if (stockCountryId != null) {
            return countryRepo.findById(stockCountryId)
                .orElseThrow(() -> OtakuException.notFound("Pays", stockCountryId));
        }

        return countryRepo.findByActiveTrue().stream().findFirst().orElse(null);
    }

    private void saveProductImages(Product product, List<MultipartFile> files) throws IOException {
        boolean hasPrimary = imageRepo.findByProductIdAndPrimaryTrue(product.getId()).isPresent();
        for (int i = 0; i < files.size(); i++) {
            var result = cloudinaryService.uploadProductImage(files.get(i), product.getId());
            if (!result.isUploaded()) {
                log.warn("Image produit ignoree car Cloudinary est indisponible - productId={} fileIndex={}", product.getId(), i);
                continue;
            }
            ProductImage image = ProductImage.builder()
                .product(product)
                .url(result.url())
                .primary(!hasPrimary && i == 0)
                .sortOrder(i)
                .build();
            imageRepo.save(image);
            if (!hasPrimary && i == 0) {
                hasPrimary = true;
            }
        }
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");
        String slug = base;
        int i = 1;
        while (productRepo.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            return cloudinaryUrl;
        }
        int uploadIdx = cloudinaryUrl.indexOf("/upload/");
        if (uploadIdx == -1) {
            return cloudinaryUrl;
        }
        String path = cloudinaryUrl.substring(uploadIdx + 8);
        path = path.replaceFirst("v\\d+/", "");
        int dotIdx = path.lastIndexOf('.');
        return dotIdx != -1 ? path.substring(0, dotIdx) : path;
    }

    private Long resolveAccessibleCountryId(Long countryId, Long cityId, OurUser requester) {
        if (requester == null || requester.getRole() != Role.OWNER) {
            return countryId;
        }

        if (requester.getCountry() == null) {
            throw OtakuException.forbidden("Compte owner sans pays rattache.");
        }

        if (countryId != null && !requester.getCountry().getId().equals(countryId)) {
            throw OtakuException.forbidden("Un owner ne peut consulter que les produits de son pays.");
        }

        if (requester.getCity() != null) {
            if (cityId == null) {
                throw OtakuException.forbidden("Le cityId est requis pour un owner.");
            }

            if (!requester.getCity().getId().equals(cityId)) {
                throw OtakuException.forbidden("Un owner ne peut consulter que les produits de sa ville.");
            }
        }

        return requester.getCountry().getId();
    }
}
