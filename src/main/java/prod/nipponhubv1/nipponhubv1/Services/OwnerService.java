package prod.nipponhubv1.nipponhubv1.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.Response.CountryKpi;
import prod.nipponhubv1.nipponhubv1.Dto.Response.CountryKpiDetailResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.GlobalKpiResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.OwnerShareCalculation;
import prod.nipponhubv1.nipponhubv1.Dto.Response.TopProductKpi;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.OwnerShare;
import prod.nipponhubv1.nipponhubv1.Models.WhatsappContact;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateCommissionRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CityRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyAccountRepository;
import prod.nipponhubv1.nipponhubv1.Repository.OrderItemRepository;
import prod.nipponhubv1.nipponhubv1.Repository.OrderRepository;
import prod.nipponhubv1.nipponhubv1.Repository.OwnerShareRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;
import prod.nipponhubv1.nipponhubv1.Repository.WhatsappContactRepository;

/**
 * Tableau de bord et configuration réservés à l'OWNER.
 *
 * Responsabilités :
 *  - KPIs globaux (CA, commandes, clients, top produits)
 *  - Filtrage par pays / ville / période
 *  - Calcul de la part réelle Owner (CA - commissions affiliés - coûts)
 *  - Configuration globale (pays actifs, villes, quartiers/frais, WhatsApp)
 *  - Gestion des parts Owner par pays
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OrderRepository           orderRepo;
    private final OrderItemRepository       orderItemRepo;
    private final UserRepository            userRepo;
    private final CountryRepository         countryRepo;
    private final CityRepository            cityRepo;
    private final WhatsappContactRepository whatsappRepo;
    private final OwnerShareRepository      ownerShareRepo;
    private final AffiliateCommissionRepository commissionRepo;
    private final LoyaltyAccountRepository  loyaltyRepo;

    // ── KPIs & Statistiques ───────────────────────────────────────────────────

    /**
     * KPIs globaux toutes périodes et tous pays confondus.
     */
    @Transactional(readOnly = true)
    public GlobalKpiResponse getGlobalKpis(LocalDateTime from, LocalDateTime to) {
        List<Country> countries = countryRepo.findByActiveTrue();

        BigDecimal totalRevenue   = BigDecimal.ZERO;
        long       totalOrders    = 0;
        long       totalCustomers = userRepo.findByRoleAndCountryId(
            Role.CLIENT, null, Pageable.unpaged()
        ).getTotalElements();

        // Agréger par pays
        List<CountryKpi> countryKpis = new ArrayList<>();
        for (Country country : countries) {
            BigDecimal revenue = orderRepo
                .sumRevenueByCountryAndPeriod(country.getId(), from, to)
                .orElse(BigDecimal.ZERO);

            long orders = orderRepo
                .findByCountryIdOrderByCreatedAtDesc(country.getId(), Pageable.unpaged())
                .getTotalElements();

            totalRevenue = totalRevenue.add(revenue);
            totalOrders += orders;

            countryKpis.add(CountryKpi.builder()
                .countryId(country.getId())
                .countryName(country.getName())
                .countryCode(country.getCode())
                .currency(country.getCurrency())
                .revenue(revenue)
                .totalOrders(orders)
                .build());
        }

        return GlobalKpiResponse.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .totalCustomers(totalCustomers)
            .periodFrom(from)
            .periodTo(to)
            .byCountry(countryKpis)
            .build();
    }

    /**
     * KPIs détaillés d'un pays sur une période.
     */
    @Transactional(readOnly = true)
    public CountryKpiDetailResponse getCountryKpis(Long countryId,
                                                    LocalDateTime from,
                                                    LocalDateTime to) {
        Country country = countryRepo.findById(countryId)
            .orElseThrow(() -> OtakuException.notFound("Pays", countryId));

        BigDecimal revenue = orderRepo
            .sumRevenueByCountryAndPeriod(countryId, from, to)
            .orElse(BigDecimal.ZERO);

        // Commissions affiliés payées sur la période
        BigDecimal affiliateCost = commissionRepo
            .sumConfirmedCommissions(null, from, to); // null = tous affiliés du pays

        // Part Owner configurée pour ce pays
        OwnerShare ownerShare = ownerShareRepo.findByCountryId(countryId)
            .orElse(null);
        BigDecimal ownerSharePct = ownerShare != null
            ? ownerShare.getSharePct()
            : BigDecimal.ZERO;

        BigDecimal ownerRevenue = revenue
            .multiply(ownerSharePct.divide(BigDecimal.valueOf(100)))
            .setScale(2, RoundingMode.HALF_UP);

        // Top produits vendus
        Pageable top5 = PageRequest.of(0, 5);
        List<Object[]> topProductsRaw =
            orderItemRepo.findTopProductsByCountry(countryId, from, to, top5);

        List<TopProductKpi> topProducts = topProductsRaw.stream()
            .map(row -> TopProductKpi.builder()
                .productId(((Number) row[0]).longValue())
                .productName((String) row[1])
                .totalSold(((Number) row[2]).longValue())
                .build())
            .toList();

        // Répartition des commandes par statut
        List<Object[]> statusCounts =
            orderRepo.countByStatusForCountry(countryId);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        statusCounts.forEach(row ->
            ordersByStatus.put(row[0].toString(), ((Number) row[1]).longValue())
        );

        return CountryKpiDetailResponse.builder()
            .countryId(countryId)
            .countryName(country.getName())
            .currency(country.getCurrency())
            .totalRevenue(revenue)
            .affiliateCost(affiliateCost)
            .ownerSharePct(ownerSharePct)
            .ownerRevenue(ownerRevenue)
            .ordersByStatus(ordersByStatus)
            .topProducts(topProducts)
            .periodFrom(from)
            .periodTo(to)
            .build();
    }

    /**
     * Calcule la part réelle Owner pour un pays et une période.
     * Part réelle = (CA livré × %Owner) - commissions affiliés payées
     */
    @Transactional(readOnly = true)
    public OwnerShareCalculation calculateOwnerShare(Long countryId,
                                                      LocalDateTime from,
                                                      LocalDateTime to) {
        BigDecimal revenue = orderRepo
            .sumRevenueByCountryAndPeriod(countryId, from, to)
            .orElse(BigDecimal.ZERO);

        OwnerShare share = ownerShareRepo.findByCountryId(countryId)
            .orElseThrow(() -> OtakuException.badRequest(
                "Aucune part Owner configurée pour ce pays."
            ));

        BigDecimal grossShare = revenue
            .multiply(share.getSharePct().divide(BigDecimal.valueOf(100)))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal affiliateCost = commissionRepo
            .sumConfirmedCommissions(null, from, to);

        BigDecimal netShare = grossShare.subtract(affiliateCost)
            .max(BigDecimal.ZERO);

        return OwnerShareCalculation.builder()
            .countryId(countryId)
            .totalRevenue(revenue)
            .sharePct(share.getSharePct())
            .grossShare(grossShare)
            .affiliateCost(affiliateCost)
            .netShare(netShare)
            .periodFrom(from)
            .periodTo(to)
            .build();
    }

    // ── Configuration Globale ─────────────────────────────────────────────────

    // ── Pays ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Country> getAllCountries() {
        return countryRepo.findAll();
    }

    @Transactional
    public Country createCountry(Country country) {
        if (countryRepo.existsByCode(country.getCode())) {
            throw OtakuException.conflict(
                "Un pays avec le code « " + country.getCode() + " » existe déjà."
            );
        }
        return countryRepo.save(country);
    }

    @Transactional
    public Country toggleCountryActive(Long id, boolean active) {
        Country country = countryRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Pays", id));
        country.setActive(active);
        return countryRepo.save(country);
    }

    // ── Villes ────────────────────────────────────────────────────────────────

    @Transactional
    public City createCity(Long countryId, String cityName) {
        Country country = countryRepo.findById(countryId)
            .orElseThrow(() -> OtakuException.notFound("Pays", countryId));
        City city = City.builder()
            .country(country)
            .name(cityName)
            .active(true)
            .build();
        return cityRepo.save(city);
    }

    @Transactional
    public City toggleCityActive(Long cityId, boolean active) {
        City city = cityRepo.findById(cityId)
            .orElseThrow(() -> OtakuException.notFound("Ville", cityId));
        city.setActive(active);
        return cityRepo.save(city);
    }

    @Transactional(readOnly = true)
    public List<City> getCitiesByCountry(Long countryId) {
        return cityRepo.findByCountryIdAndActiveTrue(countryId);
    }

    // ── Contacts WhatsApp ─────────────────────────────────────────────────────

    @Transactional
    public WhatsappContact addWhatsappContact(Long cityId,
                                               String number,
                                               String label) {
        City city = cityRepo.findById(cityId)
            .orElseThrow(() -> OtakuException.notFound("Ville", cityId));

        WhatsappContact contact = WhatsappContact.builder()
            .city(city)
            .whatsappNumber(number)
            .label(label)
            .active(true)
            .build();

        return whatsappRepo.save(contact);
    }

    @Transactional
    public void deleteWhatsappContact(Long contactId) {
        WhatsappContact contact = whatsappRepo.findById(contactId)
            .orElseThrow(() -> OtakuException.notFound("Contact WhatsApp", contactId));
        contact.setActive(false);
        whatsappRepo.save(contact);
    }

    @Transactional(readOnly = true)
    public List<WhatsappContact> getWhatsappByCity(Long cityId) {
        return whatsappRepo.findByCityIdAndActiveTrue(cityId);
    }

    @Transactional(readOnly = true)
    public List<WhatsappContact> getWhatsappByCountry(Long countryId) {
        return whatsappRepo.findActiveByCountryId(countryId);
    }

    // ── Parts Owner par pays ──────────────────────────────────────────────────

    /**
     * Définit ou met à jour la part Owner pour un pays.
     */
    @Transactional
    public OwnerShare setOwnerShare(Long countryId, BigDecimal sharePct) {
        if (sharePct.compareTo(BigDecimal.ZERO) <= 0
            || sharePct.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw OtakuException.badRequest("La part doit être entre 0% et 100%.");
        }

        Country country = countryRepo.findById(countryId)
            .orElseThrow(() -> OtakuException.notFound("Pays", countryId));

        OwnerShare share = ownerShareRepo.findByCountryId(countryId)
            .orElse(OwnerShare.builder().country(country).build());

        share.setSharePct(sharePct);
        OwnerShare saved = ownerShareRepo.save(share);

        log.info("Part Owner configurée — pays={} part={}%", country.getCode(), sharePct);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<OwnerShare> getAllOwnerShares() {
        return ownerShareRepo.findAll();
    }
}
