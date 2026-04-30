package prod.nipponhubv1.nipponhubv1.Models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "franchises")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Franchise {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;       // Ex: Naruto, One Piece, Dragon Ball

    @Column(length = 100, unique = true, nullable = false)
    private String slug;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "franchise")
    @JsonIgnore
    private List<Product> products = new ArrayList<>();
}
