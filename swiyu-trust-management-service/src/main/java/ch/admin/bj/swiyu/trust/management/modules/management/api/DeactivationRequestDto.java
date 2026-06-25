package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(name = "DeactivationRequest")
@AllArgsConstructor
public class DeactivationRequestDto {

    String reason;
}
