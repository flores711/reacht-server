package flores.caro.model.entities;

import jakarta.persistence.*;

@Entity
@Table(name="language")
public class Language {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name="iso_code")
    private String isoCode; // --> Limitar a solo 2 caracteres de alguna (otra) forma?
    private String name;
}
