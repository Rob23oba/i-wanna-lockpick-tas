#include "jump_calculator.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#define GRAVITY 0.4f
#define JUMP_MUL 0.45
#define EPSILON 0.0001

#define MAX_INT (((unsigned int) -1) >> 1)

int recursion = 0;
int allow_recursion = 0;

int ceiling_frame_end = 0;
float ceiling_y = -INFINITY;
float floor_y = INFINITY;
int bad_floor = 0;

float reach_limit = -INFINITY;

int stable = 0;

static inline void tick(struct jump_state *s) {
	if (s->vspeed >= 9.0001) {
		s->vspeed = 9;
	}
	s->vspeed += GRAVITY;
	float prev_y = s->y;
	s->y += s->vspeed;
	if (s->y < ceiling_y && s->frame_limit > ceiling_frame_end) {
		s->y = prev_y;
		while (s->y >= ceiling_y + 1) {
			s->y--;
		}
		s->vspeed = 0;
	}
	if (s->y >= floor_y) {
		if (bad_floor) {
			s->y = INFINITY;
			s->frame_limit = 1;
		} else {
			s->y = prev_y;
			while (s->y < floor_y - 1) {
				s->y++;
			}
			s->vspeed = 0;
		}
	}
	if (s->y > reach_limit) {
		s->reach_y = s->reach_y < s->y ? s->reach_y : s->y;
	}
	s->frame_limit--;
}

static void bruteforce_first_jump(struct jump_state s) {
	s.vspeed = -8.5f;
	int i = 0;
	while (s.frame_limit > 0) {
		bruteforce_wait(s, i);
		if (stable && recursion == allow_recursion) {
			struct jump_state s_tmp = s;
			if (s_tmp.vspeed < -EPSILON) {
				s_tmp.vspeed = (float) (s_tmp.vspeed * JUMP_MUL);
			}
			handle_jump(s_tmp, i, 0, -1, 0, 0);
		}

		// simulate one frame
		tick(&s);
		i++;
	}
}

static void bruteforce_wait(struct jump_state s, int first_jump) {
	// current state: jump released, if first_jump == 0 then jump pressed (already handled)
	int i = 0;
	if (first_jump == 0) {
		// simulate one frame
		if (first_jump == 0 && s.vspeed < -EPSILON) {
			s.vspeed = (float) (s.vspeed * JUMP_MUL);
		}
		tick(&s);
		i++;
		handle_jump(s, first_jump, i, -1, 0, 0);
	}
	while (s.frame_limit > 0) {
		bruteforce_second_jump(s, first_jump, i);

		if (i == 0 && first_jump != -1) {
			// handle release
			if (s.vspeed < -EPSILON) {
				s.vspeed = (float) (s.vspeed * JUMP_MUL);
			}
		}
		// simulate one frame
		tick(&s);
		i++;
		handle_jump(s, first_jump, i, -1, 0, 0);
	}
}

static void bruteforce_second_jump(struct jump_state s, int first_jump, int wait) {
	// current state: jump pressed (handle now), if wait == 0 then jump released
	if (s.y >= floor_y) {
		// cannot do a tiny jump (-7.0f vspeed) on the ground
		return;
	}
	s.vspeed = -7.0f;
	if (wait == 0 && first_jump != -1) {
		// handle release
		s.vspeed = (float) (s.vspeed * JUMP_MUL);
		bruteforce_end(s, first_jump, 0, 0, 0, 0);
		return;
	}
	int i = 0;
	while (s.frame_limit > 0) {
		// handle release
		struct jump_state s_tmp = s;
		if (s_tmp.vspeed < -EPSILON) {
			s_tmp.vspeed = (float) (s_tmp.vspeed * JUMP_MUL);
		}
		bruteforce_end(s_tmp, first_jump, wait, i, 0, 0);
		if (stable && recursion == allow_recursion) {
			handle_jump(s_tmp, first_jump, wait, i, 0, 0);
		}

		tick(&s);
		i++;
		handle_jump(s, first_jump, wait, i, 0, 0);
	}
	if (stable && recursion == allow_recursion) {
		struct jump_state s_tmp = s;
		if (s_tmp.vspeed < -EPSILON) {
			s_tmp.vspeed = (float) (s_tmp.vspeed * JUMP_MUL);
		}
		handle_jump(s_tmp, first_jump, wait, i, 0, 0);
	}
}

static void bruteforce_end(struct jump_state s, int first_jump, int wait, int second_jump, int releases, int end_time) {
	// simulate one frame
	tick(&s);
	end_time++;

	handle_jump(s, first_jump, wait, second_jump, releases, end_time);
	if (s.frame_limit > 0 && s.vspeed < -EPSILON) {
		bruteforce_end(s, first_jump, wait, second_jump, releases, end_time);
		s.vspeed = (float) (s.vspeed * JUMP_MUL);
		bruteforce_end(s, first_jump, wait, second_jump, releases | (1 << (end_time - 1)), end_time);
	} else if (s.frame_limit > 0) {
		bruteforce_end(s, first_jump, wait, second_jump, releases, end_time);
	}
}

int counter = 0;

static char *write_int(char *out, unsigned int i) {
	unsigned int tmp = i;
	do {
		out++;
		tmp /= 10;
	} while (tmp > 0);
	char *tmp2 = out;
	do {
		*--out = '0' + i % 10;
		i /= 10;
	} while (i > 0);
	return tmp2;
}

#ifdef _MSC_VER
#include <intrin.h>

static inline int ctz(int num) {
	int value;
	_BitScanForward(&value, num);
	return value;
}  
#else
#define ctz(x) __builtin_ctz(x)
#endif

float min_max_y[8][2];
int max_results = 1000;
int max_leftover = MAX_INT;
float reach_required = INFINITY;

static float min_end_y(struct jump_state s) {
	float y = s.y;
	while (s.vspeed + GRAVITY < 0) {
		tick(&s);
		y = y < s.y ? y : s.y;
	}
	return y;
}

static void handle_jump(struct jump_state s, int first_jump, int wait, int second_jump, unsigned int releases, int end_time) {
	if (s.frame_limit > max_leftover) {
		return;
	}
	if (recursion == allow_recursion && s.reach_y > reach_required) {
		return;
	}
	float min_y = min_max_y[recursion][0];
	float max_y = min_max_y[recursion][1];
	if (s.y < min_y || s.y > max_y) {
		return;
	}
	if (stable && recursion == allow_recursion) {
		// check stability
		int has_stable = 0;

		float end_y = min_end_y(s);
		if (end_y >= min_y && s.y - end_y <= 1) {
			has_stable = 1;
			if (counter < max_results) {
				printf("stable, min y %.8g:\n", end_y);
			}
		}
		if (end_time == 0) {
			// no releases possible
			goto end_stable;
		}

		struct jump_state s_tmp = s;
		int i = 0;
		while (s_tmp.vspeed + GRAVITY < 0) {
			float vspeed_tmp = s_tmp.vspeed;
			s_tmp.vspeed = (float) (vspeed_tmp * JUMP_MUL);
			float end_y = min_end_y(s_tmp);
			s_tmp.vspeed = vspeed_tmp;
			if (end_y >= min_y && s.y - end_y <= 1) {
				has_stable = 1;
				if (counter < max_results) {
					printf("stable %d, min y %.8g:\n", i, end_y);
				}
			}
			tick(&s_tmp);
			i++;
			if (s_tmp.y < min_y || s.y - s_tmp.y > 1) {
				goto end_stable;
			}
		}
end_stable:
		if (!has_stable) {
			return;
		}
	}
	if (recursion < allow_recursion) {
		if (end_time == 0) return;
		int old_counter = counter;
		recursion++;
		bruteforce_first_jump(s);
		recursion--;
		if (old_counter == counter) {
			return;
		}
	}
	counter++;
	if (counter > max_results) {
		return;
	}

	char out[1024];
	char *ptr = out;
	int rec = recursion;
	while (rec) {
		*ptr++ = ' ';
		rec--;
	}
	if (first_jump != -1) {
		*ptr++ = 'J';
		*ptr++ = ' ';
		ptr = write_int(ptr, first_jump);
		*ptr++ = ' ';
		*ptr++ = 'j';
		*ptr++ = ' ';
	}
	if (wait != -1) {
		ptr = write_int(ptr, wait);
	} else {
		*ptr++ = '!';
	}
	if (second_jump != -1) {
		*ptr++ = ' ';
		*ptr++ = 'J';
		*ptr++ = ' ';
		ptr = write_int(ptr, second_jump);
		*ptr++ = ' ';
		*ptr++ = 'j';
	}
	while (releases != 0) {
		*ptr++ = ' ';
		int nmb = ctz(releases);
		ptr = write_int(ptr, nmb + 1);
		*ptr++ = ' ';
		*ptr++ = 'J';
		*ptr++ = 'j';
		releases >>= nmb + 1;
		end_time -= nmb + 1;
	}
	*ptr++ = ' ';
	ptr = write_int(ptr, end_time);
	*ptr++ = '\0';
	printf("%s: %.8g %.8g\n", out, s.y, s.vspeed);
}

static int parse_args(int argc, char **argv, float *y, float *vspeed, int *wait_first, int *max_frames) {
	if (argc < 3) {
		fprintf(stderr, "%s requires at least 2 arguments\n", argv[0]);
		return 1;
	}
	*y = strtof(argv[1], 0);
	*max_frames = strtol(argv[2], 0, 10);
	int i = 3;
	while (i < argc) {
		char *arg = argv[i++];
		if (strcmp(arg, "--help") == 0) {
			return 1;
		} else if (strcmp(arg, "--min-frames") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--min-frames requires <frames> parameter\n");
				return 1;
			}
			max_leftover = *max_frames - strtol(argv[i++], 0, 10);
		} else if (strcmp(arg, "--stable") == 0) {
			stable = 1;
		} else if (strcmp(arg, "--range") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--range requires <jump_height>p or <min_y> and <max_y> parameters\n");
				return 1;
			}
			size_t l = strlen(argv[i]);
			if (l > 0 && argv[i][l - 1] == 'p') {
				float startPos = *y;
				float rounded = rintf(startPos - 21.0f) + 21.0f;
				float height = strtof(argv[i++], 0);
				float midPoint = rounded - height;
				min_max_y[allow_recursion][0] = midPoint - 0.5f;
				min_max_y[allow_recursion][1] = midPoint + 0.5f;
			} else if (i + 2 > argc) {
				fprintf(stderr, "--range requires <jump_height>p or <min_y> and <max_y> parameters\n");
				return 1;
			} else {
				min_max_y[allow_recursion][0] = strtof(argv[i++], 0);
				min_max_y[allow_recursion][1] = strtof(argv[i++], 0);
			}
		} else if (strcmp(arg, "--reach") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--reach requires <y> parameter\n");
				return 1;
			}
			reach_required = strtof(argv[i++], 0);
		} else if (strcmp(arg, "--reach-max") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--reach-max requires <y> parameter\n");
				return 1;
			}
			reach_limit = strtof(argv[i++], 0);
		} else if (strcmp(arg, "--in-air-velocity") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--in-air-velocity requires <vspeed> parameter\n");
				return 1;
			}
			*vspeed = strtof(argv[i++], 0);
			*wait_first = 1;
		} else if (strcmp(arg, "--floor") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--floor requires <y> parameter\n");
				return 1;
			}
			floor_y = strtol(argv[i++], 0, 10) + 0.5f;
		} else if (strcmp(arg, "--bad-floor") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--bad-floor requires <min_y> parameter\n");
				return 1;
			}
			floor_y = strtof(argv[i++], 0);
			bad_floor = 1;
		} else if (strcmp(arg, "--ceiling") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--floor requires <y> parameter\n");
				return 1;
			}
			ceiling_y = strtol(argv[i++], 0, 10) - 0.5f;
			ceiling_frame_end = 0;
		} else if (strcmp(arg, "--ceiling-end") == 0) {
			if (i + 1 > argc) {
				fprintf(stderr, "--ceiling-end requires <frames> parameter\n");
				return 1;
			}
			ceiling_frame_end = *max_frames - strtol(argv[i++], 0, 10);
		} else if (strcmp(arg, "--recurse") == 0) {
			if (i + 2 > argc) {
				fprintf(stderr, "--recurse requires <min_y> and <max_y> parameters\n");
				return 1;
			}
			if (allow_recursion >= 7) {
				fprintf(stderr, "--recurse may only be used a maximum of 7 times\n");
				return 1;
			}
			min_max_y[allow_recursion + 1][0] = min_max_y[allow_recursion][0];
			min_max_y[allow_recursion + 1][1] = min_max_y[allow_recursion][1];
			min_max_y[allow_recursion][0] = strtof(argv[i++], 0);
			min_max_y[allow_recursion][1] = strtof(argv[i++], 0);
			allow_recursion++;
		} else if (strcmp(arg, "--all-results") == 0) {
			max_results = MAX_INT;
		} else {
			fprintf(stderr, "Invalid option: %s\n", arg);
			return 1;
		}
	}
	return 0;
}

static void show_help(char *str) {
	fprintf(stderr,
		"Usage: %s <start_y> <max_frames> [options]\n"
		"Options:\n"
		"  --help: Show this help message\n"
		"  --all-results: Don't stop printing after 1000 results\n"
		"  --stable: Only show stable jumps (i.e. jumps that stay in the range or below)\n"
		"  --min-frames <frames>: Minimum amount of frames the jump needs to have\n"
		"  --range <min_y> <max_y>: Range that the jump needs to end in\n"
		"  --range <jump_height>p: Calculates the range based on the given jump height (shorthand)\n"
		"  --reach <y>: Coordinate that the jump needs to reach\n"
		"  --reach-max <y>: Coordinate that the jump needs to reach\n"
		"  --in-air-velocity <vspeed>: Indicates that the jump begins in the air with the given velocity\n"
		"  --floor <y>: Maximum y coordinate (integer) until hitting floor\n"
		"  --bad-floor <min_y>: Jumps should not reach this y coordinate\n"
		"  --ceiling <y>: Minimum y coordinate (integer) until hitting ceiling\n"
		"  --ceiling-end <frame>: Amount of frames until there is no ceiling anymore\n"
		"  --recurse <floor_min_y> <floor_max_y>: Do another jump after the first (can be repeated)\n", str);
}

int main(int argc, char **argv) {
	float y, vspeed;
	int wait_first = 0;
	int max_frames;
	int error = parse_args(argc, argv, &y, &vspeed, &wait_first, &max_frames);
	if (error) {
		show_help(argv[0]);
		return 1;
	}

	struct jump_state s;
	if (wait_first) {
		s.y = y;
		s.vspeed = vspeed;
		s.frame_limit = max_frames;
		s.reach_y = INFINITY;
		bruteforce_wait(s, -1);
	} else {
		s.y = y;
		s.vspeed = 0;
		s.frame_limit = max_frames;
		s.reach_y = INFINITY;
		bruteforce_first_jump(s);
	}
	if (counter > max_results) {
		printf("... %d more results\n", counter - max_results);
	}
	printf("Jump count: %d\n", counter);
	return 0;
}
