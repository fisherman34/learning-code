package com.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        // File names of current / orginal file
        String originalFileName = file.getOriginalFilename();
        // Generate a unique file name for the uploaded image
        String randomId = UUID.randomUUID().toString();
        // UUIDで生成したランダムなIDに、元のファイルの拡張子を付ける。
        // 例えば、originalFileName が「photo.jpg」の場合、
        // randomId が「550e8400-e29b-41d4-a716-446655440000」なら、
        // 「550e8400-e29b-41d4-a716-446655440000.jpg」というファイル名になる。
        //
        // originalFileName.lastIndexOf(".") → ファイル名の中で、最後の . の位置を取得する
        // substring(...) → . 以降を切り出す（拡張子を取得する）
        //　
        // randomId.concat(".jpg") → randomId の後ろに .jpg を連結する
        String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf(".")));

        // File.separator     → ファイルパスの区切り
        String filePath = path + File.separator + fileName;

        // Check if the directory exists, if not create it
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Upload to server
        Files.copy(file.getInputStream(), Paths.get(filePath));
        // Return the file name of the uploaded image
        return fileName;
    }
}
