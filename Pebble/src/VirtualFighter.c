//block - everything decreases
//punch - everything decreases


#include "pebble.h"

#define HISTORY 20

static Window *window;

TextLayer *healthBar;

char accelTextArr[50];

char maxTextArr[50];
int maxX=0, maxY=0, maxZ=0;

char minTextArr[50];
int minX=0, minY=0, minZ=0;

TextLayer *healthTextBottom;
TextLayer *healthText;
char healthTextArr[50];

int prevHealth=100;
static const uint32_t const segments[] = {1000, 100, 1000, 100, 1000};
VibePattern pat = {
    .durations = segments,
    .num_segments = ARRAY_LENGTH(segments)
};
int maxWidth,maxHeight;
/*static int abs(int x){
    if(x<0)
        return -x;
    else
        return x;
}*/

static void checkMinMax(int x, int y, int z){
    if(x>maxX){
            maxX=x;
        }
        if(y>maxY){
            maxY=y;
        }
        if(z>maxZ){
            maxZ=z;
        }
        if(x<minX){
            minX=x;
        }
        if(y<minY){
            minY=y;
        }
        if(z<minZ){
            minZ=z;
        }
}

static int isPunch(int *avg){
    if(avg[1]<avg[2]&&avg[1]<avg[3])
        return true;
    else
        return false;
}

static int isBlock(int *avg){
    if((avg[1]<900&&avg[3]<avg[2]&&avg[3]<500)||(avg[3]<avg[1]&&avg[3]<avg[2]&&avg[0]<900))
        return true;
    else
        return false;
}

float my_sqrt(int num)
{
    float a, p, e = 0.001, b;
    a = num;
    p = a * a;
    int nb = 0;
    while ((p - num >= e) && (nb++ < 40)) {
        b = (a + (num / a)) / 2;
        a = b;
        p = a * a;
    }
    return a;
}

void send_int(uint8_t key, uint8_t cmd)
{
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    Tuplet value = TupletInteger(key, cmd);
    dict_write_tuplet(iter, &value);
    app_message_outbox_send();
}
static int majorDrop(int *avg){
    return avg[0]<900;
}
static void averageAccel(AccelRawData *data, int num_samples, int *avg){
    if(num_samples==0)
        return;
    int total[]={0,0,0};
    for(int i=0;i<num_samples;i++){
        int x=data[i].x,y=data[i].y,z=data[i].z;
        checkMinMax(x,y,z);
        total[0]+=x;
        total[1]+=y;
        total[2]+=z;
    }
    total[0]/=num_samples;
    total[1]/=num_samples;
    total[2]/=num_samples;
    avg[0]=my_sqrt(total[0]*total[0]+total[1]*total[1]+total[2]*total[2]);
    avg[1]=my_sqrt(total[0]*total[0]+total[1]*total[1]);
    avg[2]=my_sqrt(total[0]*total[0]+total[2]*total[2]);
    avg[3]=my_sqrt(total[1]*total[1]+total[2]*total[2]);    
    if(isBlock(avg)){
        APP_LOG(APP_LOG_LEVEL_INFO,"block!");
        send_int(0,0);
    }
    else if(isPunch(avg)&&majorDrop(avg)){
        APP_LOG(APP_LOG_LEVEL_INFO,"punch!");
        send_int(0,1);
    }
}

static void accelSubscriber(AccelRawData *data, uint32_t num_samples, uint64_t timestamp){
    int avg[]={0,0,0,0};
    averageAccel(data,num_samples,avg);
    snprintf(accelTextArr, 50, "mag=%d xy=%d xz=%d yz=%d", avg[0],avg[1],avg[2],avg[3]);
    // snprintf(maxTextArr,50,"Max: x=%d y=%d z=%d",maxX,maxY,maxZ);
    // snprintf(minTextArr,50,"Min: x=%d y=%d z=%d",minX,minY,minZ);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%d",(int)num_samples);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",accelTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",maxTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",minTextArr);
}

void process_tuple(Tuple *t)
{
    //int key = t->key;
    int value = t->value->int32;
    if(value<=0){
        vibes_enqueue_custom_pattern(pat);
    }else if(prevHealth-value<=4){
        vibes_short_pulse();
    }else if(prevHealth-value>4){
        vibes_double_pulse();
    }
    if(value<=0){
        snprintf(healthTextArr, 50, "You're dead!!!");
    }else{
        snprintf(healthTextArr, 50, "Health=%d", value);
    }
    text_layer_set_text(healthText, healthTextArr);
    text_layer_set_text(healthTextBottom, healthTextArr);
    int prop=maxHeight*value/100;
    layer_set_frame((Layer*)healthBar,GRect(0,maxHeight-prop,144,prop));
    text_layer_set_background_color(healthBar, GColorBlack);
    prevHealth=value;
}

static void in_received_handler(DictionaryIterator *iter, void *context)
{
    Tuple *t = dict_read_first(iter);
    while(t != NULL)
    {
        process_tuple(t);
        t = dict_read_next(iter);
    }
}

static void window_load(Window *window) {
    healthText=text_layer_create(GRect(0,0,144,25));
    healthTextBottom=text_layer_create(GRect(0,120,144,25));
    GRect rect=layer_get_frame(window_get_root_layer(window));
    maxWidth=rect.size.w;
    maxHeight=rect.size.h;
    healthBar = text_layer_create(rect);
    text_layer_set_background_color(healthBar, GColorBlack);
    text_layer_set_background_color(healthText, GColorClear);
    text_layer_set_background_color(healthTextBottom, GColorClear);
    text_layer_set_text_color(healthText, GColorBlack);
    text_layer_set_text_color(healthTextBottom, GColorWhite);
    text_layer_set_text(healthText,"Starting up...");
    text_layer_set_text(healthTextBottom,"Starting up...");
    layer_add_child(window_get_root_layer(window), (Layer*)healthBar);
    layer_add_child(window_get_root_layer(window), (Layer*)healthText);
    layer_add_child(window_get_root_layer(window), (Layer*)healthTextBottom); 
}

static void window_unload(Window *window) {
    text_layer_destroy(healthText);
    text_layer_destroy(healthTextBottom);
}

static void init(void) {
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
      .load = window_load,
      .unload = window_unload
    });
    app_message_register_inbox_received(in_received_handler);
    app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());  
    accel_service_set_sampling_rate(ACCEL_SAMPLING_50HZ);
    accel_raw_data_service_subscribe(HISTORY, accelSubscriber);
    window_stack_push(window, true);
}

static void deinit(void) {
    accel_data_service_unsubscribe();
    window_destroy(window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
