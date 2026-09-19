/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The order of an atlas's slots - the rule a writer and a reader have to read the
 * same way, on pain of drawing every item after the first slim one with somebody
 * else's pixels.
 */
class AtlasTest {

    private static final List<Atlas.Item> ITEMS = List.of(
            new Atlas.Item("cap", false),
            new Atlas.Item("crown", true),
            new Atlas.Item("helm", false));

    @Test
    void a_slim_variant_follows_the_item_it_belongs_to() {
        assertThat(Atlas.slots("hats", ITEMS)).containsExactly(
                "hats/cap", "hats/crown", "hats/crown@slim", "hats/helm");
    }

    @Test
    void the_rank_of_a_slot_is_its_rank_in_the_atlas() {
        List<String> slots = Atlas.slots("hats", ITEMS);

        // What a reader needs: the item's own rank runs ahead of its rank in the list.
        assertThat(slots.indexOf("hats/helm")).isEqualTo(3);
        assertThat(slots).hasSize(Atlas.slots("hats", ITEMS).size());
    }

    @Test
    void a_category_without_items_has_no_slots() {
        assertThat(Atlas.slots("hats", List.of())).isEmpty();
    }

    @Test
    void a_slot_is_named_the_way_both_clients_name_it() {
        assertThat(Atlas.slot("hair", "bob", false)).isEqualTo("hair/bob");
        assertThat(Atlas.slot("hair", "bob", true)).isEqualTo("hair/bob@slim");
    }

    @Test
    void a_slot_holds_one_skin_buffer() {
        assertThat(Atlas.SLOT_BYTES).isEqualTo(Model.SIZE * Model.SIZE * 4);
    }
}
