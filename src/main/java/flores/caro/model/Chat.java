package flores.caro.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
