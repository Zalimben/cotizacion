import {createDecorator} from "vue-class-component";
import {MetaInfo} from "vue-meta";

export const Meta = function t(data: MetaInfo) {
    return createDecorator((options: any) => {
        options.head = data;
    });
};
