package com.greeting.feature.greeting.entity;

import com.greeting.common.entity.UUIDEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "greeting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GreetingEntity extends UUIDEntityBase {

    @Column(name = "text", nullable = false, length = 40)
    private String text;
}
