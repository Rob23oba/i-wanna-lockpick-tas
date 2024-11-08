#ifndef JUMP_CALCULATOR_H
#define JUMP_CALCULATOR_H

struct jump_state {
	float y;
	float vspeed;
	int frame_limit;
	float reach_y;
};

static void bruteforce_first_jump(struct jump_state s);
static void bruteforce_wait(struct jump_state s, int first_jump);
static void bruteforce_second_jump(struct jump_state s, int first_jump, int wait);
static void bruteforce_end(struct jump_state s, int first_jump, int wait, int second_jump, int releases, int end_time);

static void handle_jump(struct jump_state s, int first_jump, int wait, int second_jump, unsigned int releases, int end_time);

#endif
