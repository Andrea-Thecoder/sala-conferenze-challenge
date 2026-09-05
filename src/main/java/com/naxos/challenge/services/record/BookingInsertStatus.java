package com.naxos.challenge.services.record;

import java.util.UUID;

public record BookingInsertStatus(UUID conferenceHallId, String errorMessage) {
}
