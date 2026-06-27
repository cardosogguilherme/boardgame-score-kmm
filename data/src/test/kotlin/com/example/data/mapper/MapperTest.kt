package com.example.data.mapper

import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {
    @Test fun template_round_trips_through_entities() {
        val (t, fields, rules) = DeepSea.template.toEntities()
        val restored = templateFrom(t, fields, rules)
        assertEquals(DeepSea.template, restored)
    }
}
