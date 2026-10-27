package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entry {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    @Setter(AccessLevel.NONE)
    private String title;

    @Column
    private Double score;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EntryStatus status = EntryStatus.UNPLAYED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EntryOwnership ownership;

    @Column
    private Integer playtime;

    public Entry(String title) {
        rename(title);
    }

    public void setStatus(EntryStatus status) {
        this.status = status == null ? EntryStatus.UNPLAYED : status;
    }

    public void rename(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The title is mandatory");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("The title can't be longer than 200 characters");
        }

        this.title = title.strip();
    }

}
