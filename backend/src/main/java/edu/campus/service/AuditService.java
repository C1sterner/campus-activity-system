package edu.campus.service;

import edu.campus.domain.Models.User;
import edu.campus.mapper.AppMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AppMapper mapper;
    private static final Logger LOG=LoggerFactory.getLogger(AuditService.class);
    public AuditService(AppMapper mapper){this.mapper=mapper;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(User actor,String type,String content,String ip){
        try{mapper.log(actor.userId,type,content,ip);}catch(Exception e){LOG.warn("Audit unavailable for action {}",type);}
    }
}
