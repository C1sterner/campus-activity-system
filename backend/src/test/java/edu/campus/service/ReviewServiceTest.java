package edu.campus.service;

import edu.campus.common.ApiException;
import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock AppMapper mapper;
    @InjectMocks ReviewService service;
    User teacher; Registration registration; Activity activity;
    @BeforeEach void setup(){
        teacher=new User();teacher.userId=2001L;teacher.role="teacher";
        registration=new Registration();registration.regId=17;registration.activityId=101;registration.status="1";registration.version=0;
        activity=new Activity();activity.activityId=101;activity.reviewerId=2001L;
        lenient().when(mapper.registration(17)).thenReturn(registration);
        lenient().when(mapper.activity(101)).thenReturn(activity);
        lenient().when(mapper.updateReviewStatus(eq(17),anyString(),eq(0))).thenReturn(1);
    }
    private ApiException error(String result,String opinion,int status){ApiException e=assertThrows(ApiException.class,()->service.submitReview(teacher,17,result,opinion,0));assertEquals(status,e.status);verify(mapper,never()).insertReview(any());return e;}
    @Test void W01_approve(){
        var result=service.submitReview(teacher,17,"1",null,0);
        assertEquals("2",result.status());assertEquals(1,result.version());
        verify(mapper).updateReviewStatus(17,"2",0);
        ArgumentCaptor<ReviewRecord> record=ArgumentCaptor.forClass(ReviewRecord.class);verify(mapper).insertReview(record.capture());
        assertEquals("1",record.getValue().result);assertEquals(2001L,record.getValue().reviewerId);assertNull(record.getValue().opinion);
    }
    @Test void W02_invalidResult(){error("3","ok",400);verify(mapper,never()).registration(anyInt());}
    @Test void W03_opinionTooLong(){error("1","a".repeat(501),400);verify(mapper,never()).registration(anyInt());}
    @Test void W04_blankRejection(){error("2","   ",400);verify(mapper,never()).registration(anyInt());}
    @Test void W05_missingRegistration(){when(mapper.registration(17)).thenReturn(null);error("1","ok",404);}
    @Test void W06_wrongReviewer(){activity.reviewerId=9999L;error("1","ok",403);verify(mapper,never()).updateReviewStatus(anyInt(),anyString(),anyInt());}
    @Test void W07_reject(){var result=service.submitReview(teacher,17,"2"," 材料不完整 ",0);assertEquals("3",result.status());ArgumentCaptor<ReviewRecord> c=ArgumentCaptor.forClass(ReviewRecord.class);verify(mapper).insertReview(c.capture());assertEquals("材料不完整",c.getValue().opinion);verify(mapper).updateReviewStatus(17,"3",0);}
    @Test void W08_staleVersion(){when(mapper.updateReviewStatus(17,"2",0)).thenReturn(0);assertEquals("CONFLICT_409",error("1","ok",409).code);}
    @Test void C01_nullResult(){error(null,"ok",400);}
    @Test void C02_nullRejectionOpinion(){error("2",null,400);}
    @Test void C03_noActor(){assertEquals(403,assertThrows(ApiException.class,()->service.submitReview(null,17,"1",null,0)).status);}
    @Test void C04_wrongRole(){teacher.role="student";error("1","ok",403);}
    @Test void C05_missingActivity(){when(mapper.activity(101)).thenReturn(null);error("1","ok",403);}
    @Test void B01_maximumOpinion(){service.submitReview(teacher,17,"2","a".repeat(500),0);verify(mapper).insertReview(any());}
    @Test void E01_insertFailurePropagates(){doThrow(new DataAccessResourceFailureException("test-only failure")).when(mapper).insertReview(any());assertThrows(DataAccessResourceFailureException.class,()->service.submitReview(teacher,17,"1",null,0));}
}
