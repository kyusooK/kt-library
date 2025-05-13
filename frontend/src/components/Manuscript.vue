<template>
    <div>
        <String
            label="원고제목"
            v-model="value.title"
            :editMode="editMode"
        />
        <String
            label="원고내용"
            v-model="value.content"
            :editMode="editMode"
        />
        <AuthorId
            offline
            label="authorId"
            v-model="value.authorId"
            :editMode="editMode"
            @change="change"
        />
        <Status
            offline
            label="Status"
            v-model="value.status"
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
    name: 'Manuscript',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "manuscripts",
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
