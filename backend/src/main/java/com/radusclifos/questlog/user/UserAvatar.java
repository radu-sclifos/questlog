package com.radusclifos.questlog.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_avatars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {

    @Id
    private Long userId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    // Needed to refresh the cache whenever the picture is updated
    @Column(nullable = false, length = 36)
    private String revision;

    public UserAvatar(Long userId, byte[] data) {
        this.userId = userId;
        this.data = data;
        this.revision = java.util.UUID.randomUUID().toString();
    }

}
