package edu.campus.service;

import edu.campus.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewValidationTest {
    void accepted(String result,String opinion){assertDoesNotThrow(()->ReviewValidation.check(result,opinion));}
    void refused(String result,String opinion,String message){ApiException e=assertThrows(ApiException.class,()->ReviewValidation.check(result,opinion));assertEquals(400,e.status);assertEquals(message,e.getMessage());}
    @Test void W01_validApproval(){accepted("1",null);}
    @Test void W02_illegalResult(){refused("3","说明","审核结果只能是通过或驳回");}
    @Test void W03_overlongOpinion(){refused("1","a".repeat(501),"审核意见不能超过500字符");}
    @Test void W04_blankRejectReason(){refused("2","   ","驳回时必须填写审核原因");}
    @Test void C01_nullResult(){refused(null,null,"审核结果只能是通过或驳回");}
    @Test void C02_validReject(){accepted("2","材料缺失");}
    @Test void C03_emptyApproval(){accepted("1","");}
    @Test void C04_approvalWithOpinion(){accepted("1","符合要求");}
    @Test void C05_nullRejectionReason(){refused("2",null,"驳回时必须填写审核原因");}
    @Test void B01_emptyResult(){refused("","材料缺失","审核结果只能是通过或驳回");}
    @Test void B02_spacedResult(){refused(" 1 ","说明","审核结果只能是通过或驳回");}
    @Test void B03_emptyRejectReason(){refused("2","","驳回时必须填写审核原因");}
    @Test void B04_overlongRejectReason(){refused("2","a".repeat(501),"审核意见不能超过500字符");}
    @Test void D01_oneCharacterReason(){accepted("2","缺");}
    @Test void D02_length499(){accepted("2","a".repeat(499));}
    @Test void D03_length500(){accepted("2","a".repeat(500));}
}
