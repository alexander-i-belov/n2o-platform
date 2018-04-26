package net.n2oapp.platform.jaxrs.example.api;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import org.junit.Test;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.util.Date;

/**
 * Модель данных, используемая в демонстрационном REST сервисе
 */
@Getter @Setter
@Validated
public class SomeModel {
    private Long id;
    private @NotBlank String name;
    private @Past Date date;

    public SomeModel() { }

    public SomeModel(Long id) {
        this.id = id;
    }
}