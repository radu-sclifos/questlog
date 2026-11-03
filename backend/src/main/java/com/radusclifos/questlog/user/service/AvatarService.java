package com.radusclifos.questlog.user.service;

import com.radusclifos.questlog.user.UserAvatar;
import com.radusclifos.questlog.user.repository.UserAvatarRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AvatarService {

    private final UserService users;
    private final UserAvatarRepository avatars;
    private final EntityManager em;

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;

    @Autowired
    public AvatarService(UserService users, UserAvatarRepository avatars, EntityManager em) {
        this.users = users;
        this.avatars = avatars;
        this.em = em;
    }

    public UserAvatar get(String username) {
        var user = users.find(username);
        return avatars.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found"));
    }

    @Transactional
    public String save(Principal principal, MultipartFile file) {
        var user = users.current(principal);
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image up to 2 MB");
        }

        var data = normalize(file);

        // Lock on user that has to exist
        em.lock(user, LockModeType.PESSIMISTIC_WRITE);
        var avatar = avatars.save(new UserAvatar(user.getId(), data));
        return avatar.getRevision();
    }

    @Transactional
    public void delete(Principal principal) {
        var user = users.current(principal);
        em.lock(user, LockModeType.PESSIMISTIC_WRITE);
        avatars.deleteById(user.getId());
    }

    private byte[] normalize(MultipartFile file) {
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            var reader = readers.next();
            try {
                if (!Set.of("png", "jpeg").contains(reader.getFormatName().toLowerCase(java.util.Locale.ROOT))) {
                    throw invalidImage();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) throw invalidImage();
                var source = reader.read(0);
                int side = Math.min(width, height);
                var target = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                var graphics = target.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int x = (width - side) / 2, y = (height - side) / 2;
                    graphics.drawImage(source, 0, 0, 256, 256, x, y, x + side, y + side, null);
                } finally {
                    graphics.dispose();
                }
                var output = new ByteArrayOutputStream();
                ImageIO.write(target, "png", output);
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidImage();
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid PNG or JPEG image, up to 16 megapixels");
    }

}
