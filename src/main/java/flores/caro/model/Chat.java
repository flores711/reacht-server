package flores.caro.model;

import jakarta.persistence.*;

@Entity
@Table(name="chat")
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
