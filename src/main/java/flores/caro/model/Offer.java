package flores.caro.model;

import jakarta.persistence.*;

@Entity
@Table(name="offer")
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="is_public")
    private boolean isPublic;
    private String description;
    @Column(name="is_active")
    private boolean isActive;
    @Column(name="current_players_count")
    private int currentPlayersCount;
    @Column(name="target_players_count")
    private int targetPlayersCount;

    @ManyToOne
    @JoinColumn(name="language_id")
    private Language language;

    @ManyToOne
    @JoinColumn(name="creator_id")
    private User creator;

    @ManyToOne
    @JoinColumn(name="videogame_id")
    private Videogame videogame;

    @OneToOne
    @JoinColumn(name="chat_id")
    private Chat chat;

    // Lista de jugadores


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getCurrentPlayersCount() {
        return currentPlayersCount;
    }

    public void setCurrentPlayersCount(int currentPlayersCount) {
        this.currentPlayersCount = currentPlayersCount;
    }

    public int getTargetPlayersCount() {
        return targetPlayersCount;
    }

    public void setTargetPlayersCount(int targetPlayersCount) {
        this.targetPlayersCount = targetPlayersCount;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Videogame getVideogame() {
        return videogame;
    }

    public void setVideogame(Videogame videogame) {
        this.videogame = videogame;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
}
