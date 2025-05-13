<template>
    <div>
        <Number
            label="Point"
            v-model="value.point"
            :editMode="editMode"
        />
        <Boolean
            label="구독권여부"
            v-model="value.isSubscribe"
            :editMode="editMode"
        />
        <UserId
            offline
            label="구독자정보"
            v-model="value.userId"
            :editMode="editMode"
            @change="change"
        />
        <v-row class="ma-0 pa-0">
            <v-spacer></v-spacer>
            <v-btn width="64px" color="primary" @click="save">
                저장
            </v-btn>
        </v-row>
    </div>
</template>


<script>
import BaseEntity from './base-ui/BaseEntity.vue'

export default {
    name: 'Point',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "points",
        value: {
        }
    }),
    created(){
    },
    computed:{
    },
    methods: {
    },
}
</script>
