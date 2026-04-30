package prod.nipponhubv1.nipponhubv1.Dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.*;

@Data
public class AffiliatePaymentRequest {

    @NotNull 
    private Long affiliateId;
    @NotNull 
    private LocalDate periodStart;
    @NotNull 
    private LocalDate periodEnd;
    @NotNull 
    private PaymentMethod paymentMethod;
    private String paymentProofUrl;
}
