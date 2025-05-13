<template>
    <div>
        <String
            label="리뷰내용"
            v-model="value.content"
            :editMode="editMode"
        />
        <BookId
            offline
            label="도서정보"
            v-model="value.bookId"
            :editMode="editMode"
            @change="change"
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
    name: 'Review',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "reviews",
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
