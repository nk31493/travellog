package com.side.travellog.domain.pin;

import com.side.travellog.domain.route.RouteCollaboratorRepository;
import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinPhotoService {

    private final PinPhotoRepository pinPhotoRepository;
    private final TravelPinRepository travelPinRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final int MAX_PHOTOS = 5;

    private TravelPin checkAccess(Long pinId, User user) {
        TravelPin pin = travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
        boolean isOwner = pin.getTravelRoute().getUser().getId().equals(user.getId());
        boolean isCollaborator = routeCollaboratorRepository.existsByTravelRouteAndUser(pin.getTravelRoute(), user);
        if (!isOwner && !isCollaborator) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return pin;
    }

    public PinPhoto uploadPhoto(Long pinId, String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email);
        TravelPin pin = checkAccess(pinId, user);

        if (pinPhotoRepository.countByTravelPin(pin) >= MAX_PHOTOS) {
            throw new IllegalStateException("사진은 최대 5장까지 업로드할 수 있어요.");
        }

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + ext;

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(uploadDir + fileName);
        file.transferTo(dest);

        int orderNum = pinPhotoRepository.countByTravelPin(pin);

        return pinPhotoRepository.save(PinPhoto.builder()
                .travelPin(pin)
                .fileName(originalName)
                .filePath("/uploads/" + fileName)
                .orderNum(orderNum)
                .build());
    }

    public List<PinPhoto> getPhotos(Long pinId, String email) {
        User user = userRepository.findByEmail(email);
        TravelPin pin = checkAccess(pinId, user);
        return pinPhotoRepository.findByTravelPinOrderByOrderNumAsc(pin);
    }

    public void deletePhoto(Long photoId, String email) {
        PinPhoto photo = pinPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사진입니다."));
        User user = userRepository.findByEmail(email);
        checkAccess(photo.getTravelPin().getId(), user);

        // 실제 파일 삭제
        String fileName = photo.getFilePath().replace("/uploads/", "");
        File file = new File(uploadDir + fileName);
        if (file.exists()) file.delete();

        pinPhotoRepository.delete(photo);
    }

    public void setRepresentative(Long photoId, String email) {
        PinPhoto photo = pinPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사진입니다."));
        TravelPin pin = photo.getTravelPin();
        User user = userRepository.findByEmail(email);
        checkAccess(pin.getId(), user);

        // 기존 대표 사진 해제
        pinPhotoRepository.findByTravelPinOrderByOrderNumAsc(pin)
                .forEach(p -> {
                    p.setRepresentative(false);
                    pinPhotoRepository.save(p);
                });

        // 새 대표 사진 설정
        photo.setRepresentative(true);
        pinPhotoRepository.save(photo);
    }
}