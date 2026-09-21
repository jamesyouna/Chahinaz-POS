package com.chahinaz.pos.catalogue;

import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes={ManagementCatalogueController.class,PublicCatalogueController.class,PosCatalogueController.class,com.chahinaz.pos.sales.SaleController.class,com.chahinaz.pos.operations.RegisterController.class,com.chahinaz.pos.operations.ApprovalController.class,com.chahinaz.pos.operations.ReturnController.class,com.chahinaz.pos.reporting.ReportingController.class})
public class CatalogueErrors {
  public record ApiError(Instant timestamp,int status,String message,List<String> fields) {}
  @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ApiError> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(new ApiError(Instant.now(),e.getStatusCode().value(),e.getReason(),List.of()));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    List<String> fields=e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()+": "+f.getDefaultMessage()).toList();
    return ResponseEntity.badRequest().body(new ApiError(Instant.now(),400,"Validation failed",fields));
  }
  @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ApiError> conflict(DataIntegrityViolationException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(),409,"Conflicting or invalid catalogue data",List.of()));
  }
  @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ApiError> upload(MaxUploadSizeExceededException e) {
    return ResponseEntity.badRequest().body(new ApiError(Instant.now(),400,"Image exceeds the 5 MB upload limit",List.of()));
  }
}
