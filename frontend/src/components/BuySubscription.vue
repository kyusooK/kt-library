<template>

    <v-card outlined>
        <v-card-title>
            BuySubscription
        </v-card-title>

        <v-card-text>
            <Boolean label="IsPurchase" v-model="value.isPurchase" :editMode="editMode"/>
        </v-card-text>

        <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
                    color="deep-purple lighten-2"
                    text
                    @click="buySubscription"
            >
                BuySubscription
            </v-btn>
            
            <v-btn
                    color="deep-purple lighten-2"
                    text
                    @click="close"
            >
                Close
            </v-btn>
        </v-card-actions>
    </v-card>

</template>

<script>

export default {
    name: 'BuySubscriptionCommand',
    components:{
    },
    props: {},
    data: () => ({
        editMode: true,
        value: {},
    }),
    created() {
        this.value.isPurchase = false;
    },
    watch: {
    },
    methods: {
        buySubscription() {
            this.$emit('buySubscription', this.value);
        },
        close() {
            this.$emit('closeDialog');
        },
        change() {
            this.$emit("update:modelValue", this.value);
        },
    }
}
</script>

