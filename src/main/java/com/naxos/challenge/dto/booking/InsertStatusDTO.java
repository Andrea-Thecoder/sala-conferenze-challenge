package com.naxos.challenge.dto.booking;

import com.naxos.challenge.services.record.BookingInsertStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class InsertStatusDTO {

    private List<BookingInsertStatus> bookingInsertStatuses;
    private int totalInsert;
    private int insertSuccessCount;
    private int insertFailureCount;

    public static InsertStatusDTO of(int totalInsert, int insertSuccessCount,int insertFailureCount, List<BookingInsertStatus> bookingInsertStatuses) {
        InsertStatusDTO  insertStatusDTO = new InsertStatusDTO();
        insertStatusDTO.setTotalInsert(totalInsert);
        insertStatusDTO.setInsertSuccessCount(insertSuccessCount);
        insertStatusDTO.setInsertFailureCount(insertFailureCount);
        insertStatusDTO.setBookingInsertStatuses(bookingInsertStatuses);
        return insertStatusDTO;
    }
}
