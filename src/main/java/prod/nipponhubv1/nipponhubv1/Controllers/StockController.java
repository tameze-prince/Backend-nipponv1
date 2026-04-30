package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.StockAdjustmentRequest;
import prod.nipponhubv1.nipponhubv1.Dto.StockResponse;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.StockMovement;
import prod.nipponhubv1.nipponhubv1.Services.StockService;


/**
 * GET    /api/v1/stocks?countryId=         → stocks d'un pays (ADMIN/OWNER)
 * GET    /api/v1/stocks/low?countryId=     → stocks faibles (ADMIN/OWNER)
 * GET    /api/v1/stocks/movements          → journal mouvements (ADMIN/OWNER)
 * POST   /api/v1/stocks/adjust             → ajustement manuel (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Tag(name = "Stocks", description = "Gestion des stocks par pays")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<StockResponse>> getByCountry(
            @RequestParam Long countryId,
            @RequestParam(required = false) Long cityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stockService.getStockByCountry(countryId, cityId, (OurUser) userDetails));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<StockResponse>> getLowStocks(
            @RequestParam Long countryId,
            @RequestParam(required = false) Long cityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stockService.getLowStocks(countryId, cityId, (OurUser) userDetails));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<StockMovement>> getMovements(
            @RequestParam Long variantId,
            @RequestParam Long countryId,
            @RequestParam(required = false) Long cityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(stockService.getMovements(variantId, countryId, cityId, (OurUser) userDetails));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<StockResponse> adjust(
            @Valid @RequestBody StockAdjustmentRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.ok(stockService.adjust(req, admin));
    }
}
