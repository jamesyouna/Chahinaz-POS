package com.chahinaz.pos.reporting;
import java.time.*;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
public record ReportRange(LocalDate from,LocalDate to,ZoneId zone,Instant start,Instant end){
 public static ReportRange of(LocalDate from,LocalDate to,ZoneId zone){LocalDate today=LocalDate.now(zone);LocalDate f=from==null?today.minusDays(29):from;LocalDate t=to==null?today:to;if(t.isBefore(f))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"to must be on or after from");if(java.time.temporal.ChronoUnit.DAYS.between(f,t)>366)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Date range cannot exceed 366 days");return new ReportRange(f,t,zone,f.atStartOfDay(zone).toInstant(),t.plusDays(1).atStartOfDay(zone).toInstant());}
 public ReportingDtos.Range view(){return new ReportingDtos.Range(from,to,zone.getId());}
}
