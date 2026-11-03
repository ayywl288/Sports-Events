package com.sports.modules.sportitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.mapper.EventMapper;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.modules.sportitem.service.SportItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运动项目 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class SportItemServiceImpl extends ServiceImpl<SportItemMapper, SportItem> implements SportItemService {

    private final EventMapper eventMapper;

    @Override
    public List<SportItem> getByEventId(Long eventId) {
        LambdaQueryWrapper<SportItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SportItem::getEventId, eventId)
                .eq(SportItem::getStatus, 1)
                .orderByAsc(SportItem::getId);
        return fillEventName(this.list(wrapper));
    }

    @Override
    public IPage<SportItem> pageItems(Integer current, Integer size, Long eventId, String type, Integer status) {
        Page<SportItem> page = new Page<>(current, size);

        LambdaQueryWrapper<SportItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, SportItem::getEventId, eventId)
                .eq(StringUtils.hasText(type), SportItem::getType, type)
                .eq(status != null, SportItem::getStatus, status)
                .orderByDesc(SportItem::getCreateTime);

        IPage<SportItem> result = this.page(page, wrapper);
        fillEventName(result.getRecords());
        return result;
    }

    private List<SportItem> fillEventName(List<SportItem> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        Set<Long> eventIds = items.stream()
                .map(SportItem::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (eventIds.isEmpty()) {
            return items;
        }
        Map<Long, String> nameMap = eventMapper.selectList(new LambdaQueryWrapper<Event>()
                        .in(Event::getId, eventIds))
                .stream()
                .collect(Collectors.toMap(Event::getId, Event::getName));
        items.forEach(item -> item.setEventName(nameMap.get(item.getEventId())));
        return items;
    }
}
