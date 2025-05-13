<template>
    <div>
        <String
            label="작가Email"
            v-model="value.email"
            :editMode="editMode"
        />
        <String
            label="작가명"
            v-model="value.authorName"
            :editMode="editMode"
        />
        <String
            label="작가소개"
            v-model="value.introduction"
            :editMode="editMode"
        />
        <String
            label="대표작"
            v-model="value.feturedWorks"
            :editMode="editMode"
        />
        <div variant="outlined" class="my-2">
            <Portfolios v-model="portfoliosInput"
                offline
                label="포트폴리오"
                :editMode="editMode"
                @change="change"
            />
            <v-row class="ma-0 pa-0 mt-2">
                <v-spacer></v-spacer>
                <v-btn @click="addPortfoliosList">포트폴리오 Add</v-btn>
            </v-row>
            <v-card v-if="value.portfolios.length > 0" variant="outlined" class="pa-4 mt-2">
                <v-card-sub-title>
                    포트폴리오 List
                </v-card-sub-title>
                <li v-for="(id, index) in value.portfolios" :key="index">
                    {{ id && id. ? id. : '' }}
                </li>
            </v-card>
        </div>
        <Boolean
            label="승인여부"
            v-model="value.isApprove"
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
    name: 'Author',
    mixins:[BaseEntity],
    components:{
    },
    
    data: () => ({
        path: "authors",
        portfoliosInput: null,
        value: {
            portfolios: [],
        }
    }),
    created(){
    },
    computed:{
    },
    methods: {
        addPortfoliosList() {
            if (this.portfoliosInput !== null && this.portfoliosInput !== '') {
                this.value.portfolios.push(this.portfoliosInput);
                this.portfoliosInput = null; // null로 초기화
            }
        },
    },
}
</script>
