package flores.caro.model.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="offer")
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String description;
    @Column(name="is_full")
    private boolean isFull;
    @Column(name="current_players_count")
    private int currentPlayersCount;
    @Column(name="target_players_count")
    private int targetPlayersCount;

    @ManyToOne
    @JoinColumn(name="creator_id")
    private User creator;

    @ManyToOne
    @JoinColumn(name="videogame_id")
    private Videogame videogame;

    // Para guardar instancia de chat en BD tmb al hacer persist de la oferta
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name="chat_id")
    private Chat chat;

    @OneToMany(mappedBy = "currentOffer")
    private Set<User> players = new HashSet<User>();


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
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

    public Set<User> getPlayers() {
        return players;
    }

    public void addPlayer(User player) {
        this.players.add(player);
        player.setCurrentOffer(this);
    }

    public void removePlayer(User player) {
        this.players.remove(player);
        player.setCurrentOffer(null);
    }
}
