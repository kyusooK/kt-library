<template>
    <div>
        <String
            label="구독자Email"
            v-model="value.email"
            :editMode="editMode"
        />
        <String
            label="구독자 이름"
            v-model="value.userName"
            :editMode="editMode"
        />
        <Boolean
            label="구독권 구매 여부"
            v-model="value.isPurchase"
            :editMode="editMode"
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
    name: 'User',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "users",
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
