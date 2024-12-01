package issues.issue73;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    String getUsername(@PathVariable("id") String id);
}
