<template>
    <div>
        <String
            label="표지 이미지"
            v-model="value.image"
            :editMode="editMode"
        />
        <String
            label="요약줄거리"
            v-model="value.summaryContent"
            :editMode="editMode"
        />
        <String
            label="도서명"
            v-model="value.bookName"
            :editMode="editMode"
        />
        <String
            label="PDF Path"
            v-model="value.pdfPath"
            :editMode="editMode"
        />
        <Number
            label="작가정보"
            v-model="value.authorId"
            :editMode="editMode"
        />
        <ManuscriptId
            offline
            label="작가정보"
            v-model="value.manuscriptId"
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
    name: 'Publishing',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "publishings",
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
