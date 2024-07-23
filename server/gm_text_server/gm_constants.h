#ifndef gm_constants_h
#define gm_constants_h

typedef char bool;
#define TRUE 1
#define FALSE 0

struct gm_array_1d {
	int length;
	void *rel4;
};

struct gm_array {
	int rel0;
	struct gm_array_1d *elements;
	int rel8;
	int rel12;
	int height;
};

union gm_value_union {
	double number;
	const char **string;
	struct gm_array *array;
	void *ptr;
	int i32;
	long long i64;
};

struct gm_value {
	union gm_value_union value;
	int value2;
	int type;
};

struct gm_instance_var_table_entry {
	int nameIndex1;
	struct gm_value *value;
	int nameIndex2;
};

/*struct gm_instance {
	int rel0; // = 0x6F1A14
	int rel4; // = 0
	int rel8; // = 0xBAADF00D
	int rel12; // = 0xBAADF00D
	int rel16; // = 0
	int rel20; // = 0
	int rel24; // = 0
	int rel28; // = 0
	int rel32; // = 0
	int rel36; // = 0x495
	int rel40; // = 1
	int rel44; // = 0x495
	int rel48; // = 0
	int rel52; // = 0
	int rel56; // = 0
	int rel60; // = 0
	int rel64; // = 0xBAADF00D
	int rel68; // = some number (e.g. 23 or 2 or 4)
	int rel72; // = 1
	int rel76; // = 0
	int rel80; // = 0xBAADF00D
	int rel84; // = 0xBAADF00D
	int rel88; // = 0xFFFFFF
	int rel92; // = 0 or BAADF00D
	struct gm_instance_var_table_entry *table;
	
};*/

struct gm_instance {	
	int rel0;
	int rel4;
	int rel8;
	int relc;
	int rel10;
	int rel14;
	int rel18;
	int rel1c;
	int rel20;
	int rel24;
	int rel28;
	int rel2c;
	int rel30;
	int rel34;
	int rel38;
	int rel3c;
	int rel40;
	int rel44;
	int rel48;
	int rel4c;
	int rel50;
	int rel54;
	int rel58;
	int rel5c;
	struct gm_instance_var_table_entry *var_table;
	char rel64;
	bool visible; // 65
	bool solid; // 66
	bool persistent; // 67
	int rel68;
	int rel6c;
	int rel70;
	int rel74;
	int id; // 78
	int object_index; // 7c
	int rel80;
	int rel84;
	int rel88;
	int sprite_index; // 8c
	int image_index; // 90
	float image_speed; // 94
	float image_xscale; // 98
	float image_yscale; // 9c
	float image_angle; // a0
	float image_alpha; // a4
	int rela8;
	int mask_index; // ac
	int relb0;
	float x; // b4
	float y; // b8
	float xstart; // bc
	float ystart; // c0
	float xprevious; // c4
	float yprevious; // c8
	float direction; // cc
	float speed; // d0
	float friction; // d4
	float gravity_direction; // d8
	float gravity; // dc
	float hspeed; // e0
	float vspeed; // e4
	int rele8;
	int relec;
	int relf0;
	int relf4;
	int relf8;
	int relfc;
	int rel100;
	int rel104;
	int rel108;
	int rel10c;
	int rel110;
	int rel114;
	int rel118;
	int rel11c;
	int rel120;
	int rel124;
	int path_index; // 128
	float path_position; // 12c
	float path_positionprevious; // 130
	float path_speed; // 134
	float path_scale; // 138
	float path_orientation; // 13c
	int path_endaction; // 140
	int rel144;
	int rel148;
	int timeline_index; // 14c
	int rel150;
	float timeline_position; // 154
	float timeline_speed; // 158
	bool timeline_running; // 15c
	bool timeline_loop; // 15d
	int rel160;
	int rel164;
	int rel168;
	int rel16c;
	int rel170;
	int rel174;
	int rel178;
	int rel17c;
	float depth; // 180
	int rel184;
	int rel188;
	int rel18c;
	int rel190;
	int rel194;
	int rel198;
	int rel19c;
	int rel1a0;
	int rel1a4;
	int rel1a8;
	int rel1ac;
	int rel1b0;
	int rel1b4;
	int rel1b8;
	int rel1bc;
	int rel1c0;
	int rel1c4;
	int rel1c8;
	int rel1cc;
	int rel1d0;
	int rel1d4;
	int rel1d8;
	int rel1dc;
	int rel1e0;
	int rel1e4;
	int rel1e8;
	int rel1ec;
	int rel1f0;
	int rel1f4;
	int rel1f8;
	int rel1fc;
	int rel200;
	int rel204;
	int rel208;
	int rel20c;
	int rel210;
	int rel214;
	int rel218;
	int rel21c;
	int rel220;
	int rel224;
	int rel228;
	int rel22c;
	int rel230;
	int rel234;
	int rel238;
	int rel23c;
	int rel240;
	int rel244;
	int rel248;
	int rel24c;
	int rel250;
	int rel254;
	int rel258;
	int rel25c;
	int rel260;
	int rel264;
	int rel268;
	int rel26c;
	int rel270;
	int rel274;
	int rel278;
	int rel27c;
	int rel280;
	int rel284;
	int rel288;
	int rel28c;
	int rel290;
	int rel294;
	int rel298;
	int rel29c;
	int rel2a0;
	int rel2a4;
	int rel2a8;
	int rel2ac;
	int rel2b0;
	int rel2b4;
	int rel2b8;
	int rel2bc;
	int rel2c0;
	int rel2c4;
	int rel2c8;
	int rel2cc;
	int rel2d0;
	int rel2d4;
	int rel2d8;
	int rel2dc;
	int rel2e0;
	int rel2e4;
	int rel2e8;
	int rel2ec;
	int rel2f0;
	int rel2f4;
	int rel2f8;
	int rel2fc;
	int rel300;
	int rel304;
	int rel308;
	int rel30c;
	int rel310;
	int rel314;
	int rel318;
	int rel31c;
	int rel320;
	int rel324;
	int rel328;
	int rel32c;
	int rel330;
	int rel334;
	int rel338;
	int rel33c;
	int rel340;
	int rel344;
	int rel348;
	int rel34c;
	int rel350;
	int rel354;
	int rel358;
	int rel35c;
	int rel360;
	int rel364;
	int rel368;
	int rel36c;
	int rel370;
	int rel374;
	int rel378;
	int rel37c;
	int rel380;
	int rel384;
	int rel388;
	int rel38c;
	int rel390;
	int rel394;
	int rel398;
	int rel39c;
	int rel3a0;
	int rel3a4;
	int rel3a8;
	int rel3ac;
	int rel3b0;
	int rel3b4;
	int rel3b8;
	int rel3bc;
	int rel3c0;
	int rel3c4;
	int rel3c8;
	int rel3cc;
	int rel3d0;
	int rel3d4;
	int rel3d8;
	int rel3dc;
	int rel3e0;
	int rel3e4;
};

typedef void (*gm_function)(struct gm_value *, struct gm_instance *, struct gm_instance *, int, struct gm_value *);

struct gm_texture {
	short rel0;
	short rel2;
	int rel4;
	int rel8;
	short rel12;
	short rel14;
	int rel16;
	int rel20;
};

struct gm_font_entry {
	unsigned short charCode;
	unsigned short rel2;
	unsigned short rel4;
	unsigned short index;
	short rel8;
	short width;
	short rel12;
	short rel14;
};

struct gm_font {
	int rel0; // = 0x4216f0
	const char *fontname; // = 0
	int size; // = 0
	bool bold; // = 0
	bool italic; // = 0
	int rel16; // = ???
	int rel20; // = ???
	int first; // = 0
	int last; // = 0
	int rel32; // = -1
	int rel36; // = 0
	int rel40; // = ???
	int rel44; // = ???
	int rel48; // = 0
	int rel52; // = 0
	int rel56; // = -1
	int rel60; // = 0
	int charCount; // = 0
	struct gm_font_entry **charList; // = 0
	float rel72; // = 1f
	float rel76; // = 1f
	float rel80; // = 0.5f
	int rel84; // = 0
	int rel88; // = ???
	int rel92; // = 0
	int rel96; // = 0
	int rel100; // = 0
	int rel104; // = ???
	int rel108; // = 0
	int rel112; // = 0
	int rel116; // = 0
	int rel120; // = 0
	int rel124; // = 0
	int rel128; // = 0
	int rel132; // = 0
	int rel136; // = 0
	int rel140; // = 0
};

struct gm_sprite_image {
	int rel0;
	int rel4;
	short x;
	short y;
	short width;
	short height;
};

struct gm_sprite {
	int rel0;
	int rel4;
	int rel8;
	int rel12;
	int rel16;
	int rel20;
	int imageNumber; // rel24
	int rel28;
	short rel32;
	int rel36;
	int rel40;
	int rel44;
	int rel48;
	int rel52;
	int rel56;
	int rel60;
	int rel64;
	int rel68;
	int rel72;
	int rel76;
	int rel80;
	struct gm_sprite_image **imageList;
	int rel88;
	int rel92;
	int rel96;
	int rel100;
	int rel104;
	int rel108;
	int rel112;
	int rel116;
};

struct gm_actual_code {
	int rel0;
	int rel4;
	int rel8;
	int rel12;
	int *bytecode;
};

struct gm_code {
	int rel0;
	int rel4;
	int rel8;
	int rel12;
	int rel16;
	int rel20;
	int rel24;
	int rel28;
	int rel32;
	int rel36;
	int rel40;
	int rel44;
	int rel48;
	int rel52;
	int rel56;
	int rel60;
	int rel64;
	int rel68;
	int rel72;
	int rel76;
	struct gm_actual_code *code_part;
	int rel84;
	int rel88;
	const char *name;
};

struct gm_script {
	int rel0;
	int rel4;
	struct gm_code *code;
	int rel12;
	int rel16;
	const char *name;
};

struct gm_internal_fn {
	char name[64];
	gm_function func;
	int paramCount;
	int something; // mostly 1 sometimes 0
	int aValueThing; // -1 most of the time
};

struct some_struct {
	int x1;
	int y1;
	int x2;
	int y2;
};

#define font_capacity (*(int *) 0x820ea4)
#define font_list (*(struct gm_font ***) 0x820ea8)
#define font_names (*(const char ***) 0x820eac)
#define font_count (*(int *) 0x820eb0)

#define sprite_names (*(char ***) 0xa55b0c)
#define sprite_count (*(int *) 0xa55b10)
#define sprite_list (*(struct gm_sprite ***) 0xa55b18)

#define script_list (*(struct gm_script ***) 0x846cac)
#define script_names (*(char ***) 0x846cb0)
#define script_count (*(int *) 0x846cb4)

#define internal_function_list (*(struct gm_internal_fn **) 0xa63654)
#define internal_function_count (*(int *) 0xa63658)
#define internal_function_capacity (*(int *) 0xa6365c)

#define get_font_by_id (*((struct gm_font *(*)(int)) 0x422420))

#define name_list (*(char ***) 0x7d1768)

#define TYPE_NUMBER 0
#define TYPE_STRING 1
#define TYPE_ARRAY 2
#define TYPE_PTR 3
#define TYPE_VEC3 4
#define TYPE_UNDEFINED 5
#define TYPE_OBJECT 6
#define TYPE_INT32 7
#define TYPE_VEC4 8
#define TYPE_VEC44 9
#define TYPE_INT64 10
#define TYPE_ACCESSOR 11
#define TYPE_NULL 12
#define TYPE_BOOL 13
#define TYPE_ITERATOR 14

#define gm_create_internal_function (*((void *(*)(char *, gm_function, int, bool)) 0x54f260))

#define gm_param_as_int_or_ptr (*((int (*)(struct gm_value *, int)) 0x54f610))
#define gm_param_as_bool (*((bool (*)(struct gm_value *, int)) 0x54f690))
#define gm_param_as_int (*((int (*)(struct gm_value *, int)) 0x54f750))
#define gm_param_as_uint (*((unsigned int (*)(struct gm_value *, int)) 0x54f840))
#define gm_param_as_int64 (*((long long (*)(struct gm_value *, int)) 0x54f930))
#define gm_param_as_double (*((double (*)(struct gm_value *, int)) 0x54fa50))
#define gm_param_as_ptr (*((void *(*)(struct gm_value *, int)) 0x54fb40))
#define gm_param_as_float (*((double (*)(struct gm_value *, int)) 0x54fb90))
#define gm_param_as_string (*((const char *(*)(struct gm_value *, int)) 0x550ae0))
#define gm_param_index_array (*((void (*)(struct gm_value *, struct gm_value *, int)) 0x550f90))

#define gm_show_error_message (*((void (*)(const char *, int)) 0x54ec60))
#define gm_show_error_message_formatted (*((void (*)(const char *, ...)) 0x554990))

#define gm_create_string (*((void (*)(struct gm_value *, const char *)) 0x566980))

#define GM_FUNCTION_PARAMETERS struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters

#endif