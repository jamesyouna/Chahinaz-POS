package com.chahinaz.pos.operations;
import static com.chahinaz.pos.operations.RegisterDtos.*;import jakarta.validation.Valid;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/pos/registers") public class RegisterController {private final RegisterService service;public RegisterController(RegisterService s){service=s;}
 @PostMapping("/open") @ResponseStatus(HttpStatus.CREATED) public View open(@Valid @RequestBody OpenInput in,Authentication a){return service.open(in,a);}
 @PostMapping("/{id}/close") public View close(@PathVariable UUID id,@Valid @RequestBody CloseInput in,Authentication a){return service.close(id,in,a);}
 @GetMapping("/current") public View current(Authentication a){return service.current(a);}@GetMapping public List<View> history(Authentication a){return service.history(a);}
}
