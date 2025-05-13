<template>

    <div>
        <div class="detail-title">
        Portfolio
        </div>
        <v-col>
            <String label="Category" v-model="value.category" :editMode="editMode"/>
            <String label="Works" v-model="value.works" :editMode="editMode"/>
        </v-col>

        <v-card-actions v-if="inList">
            <slot name="actions"></slot>
        </v-card-actions>
    </div>
</template>

<script>
import BaseEntity from './base-ui/BaseEntity.vue'
import BasePicker from './base-ui/BasePicker.vue'

export default {
    name: 'Portfolio',
    mixins:[BaseEntity],
    components:{
        BasePicker
    },
    data: () => ({
        path: 'Portfolios',
    }),
    props: {
    },
    watch: {
        value(val){
            this.value = val;
            this.change();
        },
    },
    async created() {
        if (Array.isArray(this.modelValue)) {
            this.value = await Promise.all(this.modelValue.map(async (item) => {
                if (item && item.id !== undefined) {
                    return await this.repository.findById(item.id);
                }
                return item;
            }));
        } else {
            this.value = this.modelValue;
            if (this.value && this.value.id !== undefined) {
                this.value = await this.repository.findById(this.value.id);
            }
        }
    },
    methods: {
        pick(val){
            this.value = val;
            this.change();
        },
    }
}
</script>

