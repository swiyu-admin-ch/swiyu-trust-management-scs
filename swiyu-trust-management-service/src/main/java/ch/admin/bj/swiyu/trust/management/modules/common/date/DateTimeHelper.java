package ch.admin.bj.swiyu.trust.management.modules.common.date;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateTimeHelper {

    public static ZonedDateTime today() {
        return Instant.now()
            .truncatedTo(ChronoUnit.DAYS) // Today always starts at start of day
            .atZone(ZoneId.of("Europe/Zurich")); // needed for relative measurements like months
    }
}
