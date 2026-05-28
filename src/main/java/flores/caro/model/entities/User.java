package flores.caro.model.entities;

import jakarta.persistence.*;

@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String email;
    private String password;
    private String description;
    private String profilephoto;

    @ManyToOne
    @JoinColumn(name="preferred_language_id")
    private Language preferredLaguage;

    @ManyToOne
    @JoinColumn(name="current_offer_id")
    private Offer currentOffer;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProfilephoto() {
        return profilephoto;
    }

    public void setProfilephoto(String profilephoto) {
        this.profilephoto = profilephoto;
    }

    public Language getPreferredLaguage() {
        return preferredLaguage;
    }

    public void setPreferredLaguage(Language preferredLaguage) {
        this.preferredLaguage = preferredLaguage;
    }

    public Offer getCurrentOffer() {
        return currentOffer;
    }

    public void setCurrentOffer(Offer currentOffer) {
        this.currentOffer = currentOffer;
    }
}
