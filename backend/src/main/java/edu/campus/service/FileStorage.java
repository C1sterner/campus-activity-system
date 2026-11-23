package edu.campus.service;

import edu.campus.common.ApiException;
import edu.campus.mapper.AppMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorage {
    private final Path root;
    private final AppMapper mapper;
    public FileStorage(@Value("${app.storage-dir}") String folder,AppMapper mapper) throws IOException {
        this.mapper=mapper;
        root=Path.of(folder).toAbsolutePath().normalize();Files.createDirectories(root);
    }
    public String save(MultipartFile file) throws IOException {
        if(file==null||file.isEmpty())return null;
        long limit=Math.min(20971520L,Long.parseLong(java.util.Optional.ofNullable(mapper.config("upload.max_size_bytes")).orElse("20971520")));
        if(file.getSize()>limit)throw new ApiException(413,"FILE_413","文件超过系统允许的大小");
        String name=file.getOriginalFilename()==null?"":file.getOriginalFilename();
        int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        if(!Set.of("jpg","png","pdf").contains(ext))throw new ApiException(415,"FILE_415","只支持jpg/png/pdf格式");
        byte[] head;try(var input=file.getInputStream()){head=input.readNBytes(8);}
        boolean valid=switch(ext){
            case "jpg" -> head.length>=3&&(head[0]&255)==255&&(head[1]&255)==216&&(head[2]&255)==255;
            case "png" -> head.length>=8&&java.util.Arrays.equals(head,new byte[]{(byte)137,80,78,71,13,10,26,10});
            default -> head.length>=5&&head[0]=='%'&&head[1]=='P'&&head[2]=='D'&&head[3]=='F'&&head[4]=='-';
        };
        String mime=file.getContentType();
        if(!valid||mime==null||!mime.equals(contentType(ext)))throw new ApiException(415,"FILE_415","文件类型与内容不一致");
        String key=UUID.randomUUID()+"."+ext;
        Path temp=Files.createTempFile(root,"upload-",".tmp");
        try {file.transferTo(temp);Files.move(temp,root.resolve(key),StandardCopyOption.ATOMIC_MOVE);}
        finally {Files.deleteIfExists(temp);}
        return key;
    }
    public Path read(String key) {
        if(key==null||!key.matches("[a-f0-9-]{36}\\.(jpg|png|pdf)"))throw ApiException.missing();
        Path file=root.resolve(key).normalize();
        if(!file.startsWith(root)||!Files.isRegularFile(file))throw ApiException.missing();
        return file;
    }
    public void discard(String key){if(key!=null)try{Files.deleteIfExists(read(key));}catch(Exception ignored){}}
    public static String contentType(String ext){return switch(ext){case "jpg"->"image/jpeg";case "png"->"image/png";default->"application/pdf";};}
}
