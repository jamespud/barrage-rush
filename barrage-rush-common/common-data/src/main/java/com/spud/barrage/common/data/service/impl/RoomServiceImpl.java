package com.spud.barrage.common.data.service.impl;

import com.spud.barrage.common.data.repository.AnchorRoomRepository;
import com.spud.barrage.common.data.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

  @Autowired
  private AnchorRoomRepository roomRepository;

}