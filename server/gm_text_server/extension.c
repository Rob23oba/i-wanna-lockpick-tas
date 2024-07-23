#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>
#include "gm_constants.h"


#define MAX_MSG_LEN 1024
#define MAX_MSG_LEN_STRING "1024"

WSADATA g_wsaData;
SOCKET g_serverSocket = INVALID_SOCKET;
SOCKET g_clientSocket = INVALID_SOCKET;
char g_inBuffer[MAX_MSG_LEN * 2];
int g_inBufferPos;

char g_outBuffer[MAX_MSG_LEN + 2];

void closeStuff() {
	if (g_clientSocket != INVALID_SOCKET) {
		closesocket(g_clientSocket);
		g_clientSocket = INVALID_SOCKET;
		return;
	}
	if (g_serverSocket != INVALID_SOCKET) {
		closesocket(g_serverSocket);
		g_serverSocket = INVALID_SOCKET;
		return;
	}
}

BOOL WINAPI DllMain(HINSTANCE hinstDll, DWORD fdwReason, LPVOID lpvReserved) {
	switch (fdwReason) {
	case DLL_PROCESS_ATTACH:
		int result = WSAStartup(MAKEWORD(2, 2), &g_wsaData);
		if (result != 0) {
			return FALSE;
		}
		break;
	case DLL_PROCESS_DETACH:
		closeStuff();
		WSACleanup();
		break;
	}
	return TRUE;
}

void gm_api_network_create_text_server(struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters) {
	if (paramCount < 2) {
		gm_show_error_message("network_create_text_server requires 2 parameters!", 1);
		return;
	}
	closeStuff();

	int status;

	const char *host = gm_param_as_string(parameters, 0);
	int port = gm_param_as_int(parameters, 1);

	SOCKADDR_IN server_address;
	server_address.sin_family = AF_INET;
	server_address.sin_port = htons(port);

	status = inet_pton(AF_INET, host, &server_address.sin_addr.s_addr);
	if (status <= 0) {
		gm_show_error_message("Connection failed!", 1);
	}

	g_serverSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (g_serverSocket == INVALID_SOCKET) {
		gm_show_error_message("Failed to create network socket!", 1);
		return;
	}

	unsigned long blockingMode = 1;
	ioctlsocket(g_serverSocket, FIONBIO, &blockingMode);

	status = bind(g_serverSocket, (SOCKADDR *) &server_address, sizeof(server_address));
	if (status == SOCKET_ERROR) {
		closesocket(g_serverSocket);
		g_serverSocket = INVALID_SOCKET;
		gm_show_error_message("Failed to bind server!", 1);
		return;
	}

	status = listen(g_serverSocket, 1);
	if (status == SOCKET_ERROR) {
		closesocket(g_serverSocket);
		g_serverSocket = INVALID_SOCKET;
		gm_show_error_message("Failed to initialize listen!", 1);
		return;
	}

	target->type = TYPE_UNDEFINED;
}

void gm_api_network_close_text_server(struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters) {
	closeStuff();
	target->type = TYPE_UNDEFINED;
}

int test_network_connection() {
	if (g_clientSocket != INVALID_SOCKET) {
		return 1;
	}
	g_clientSocket = accept(g_serverSocket, NULL, NULL);
	if (g_clientSocket == INVALID_SOCKET) {
		if (WSAGetLastError() == WSAEWOULDBLOCK) {
			return 0;
		} else {
			return -1;
		}
	}
	return 1;
}

void gm_api_network_send_text(struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters) {
	if (g_serverSocket == INVALID_SOCKET) {
		gm_show_error_message("No server initialized!", 1);
		return;
	}
	if (test_network_connection() <= 0) {
		gm_show_error_message("network_send_text requires a client to be connected!", 1);
		return;
	}
	unsigned int outBufferPos = 0;
	for (int i = 0; i < paramCount; i++) {
		unsigned int n = 0;
		switch (parameters[i].type) {
		case TYPE_NUMBER:
			n = snprintf(g_outBuffer + outBufferPos, MAX_MSG_LEN - outBufferPos, "%.17g", parameters[i].value.number);
			break;
		case TYPE_INT32:
			n = snprintf(g_outBuffer + outBufferPos, MAX_MSG_LEN - outBufferPos, "%d", parameters[i].value.i32);
			break;
		case TYPE_INT64:
			n = snprintf(g_outBuffer + outBufferPos, MAX_MSG_LEN - outBufferPos, "%lld", parameters[i].value.i64);
			break;
		case TYPE_PTR:
			n = snprintf(g_outBuffer + outBufferPos, MAX_MSG_LEN - outBufferPos, "%p", parameters[i].value.ptr);
			break;
		default:
			const char *str = gm_param_as_string(parameters, i);
			n = strlen(str);
			if (MAX_MSG_LEN <= outBufferPos +  + 1) {
				goto msg_too_long;
			}
			memcpy(g_outBuffer + outBufferPos, str, n);
			outBufferPos += n;
			continue;
		}
		if (MAX_MSG_LEN <= outBufferPos + n) {
msg_too_long:
			gm_show_error_message("Message is too long, only messages up to length " MAX_MSG_LEN_STRING " are allowed.", 1);
			return;
		}
		outBufferPos += n;
	}
	g_outBuffer[outBufferPos] = '\n';

	send(g_clientSocket, g_outBuffer, outBufferPos + 1, 0);
	target->type = TYPE_UNDEFINED;
}

int test_buffer_string(struct gm_value *target) {
	for (int i = 0; i < g_inBufferPos; i++) {
		if (g_inBuffer[i] == '\n' || g_inBuffer[i] == '\0') {
			char *ptr = g_inBuffer + i;
			if (i > 0 && *(ptr - 1) == '\r') {
				ptr--;
			}
			*ptr = '\0';
			gm_create_string(target, g_inBuffer);
			memmove(g_inBuffer, g_inBuffer + i + 1, g_inBufferPos - i - 1);
			g_inBufferPos -= i + 1;
			return 1;
		}
	}
	return 0;
}

void gm_api_network_check_text(struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters) {
	if (g_serverSocket == INVALID_SOCKET) {
		gm_show_error_message("No server initialized!", 1);
		return;
	}
	unsigned long blockingMode = 1;
	if (paramCount >= 1) {
		blockingMode = gm_param_as_int(parameters, 0) == 0;
	}
	if (test_buffer_string(target)) {
		return;
	}
back_here:
	int status = test_network_connection();
	if (status == 0) {
		target->type = TYPE_UNDEFINED;
		return;
	} else if (status < 0) {
		gm_show_error_message("An error occurred while trying to get a client!", 1);
		return;
	}

	ioctlsocket(g_clientSocket, FIONBIO, &blockingMode);

read_stuff:
	int bytes_received = recv(g_clientSocket, g_inBuffer + g_inBufferPos, MAX_MSG_LEN - g_inBufferPos, 0);
	if (bytes_received == -1) {
		if (WSAGetLastError() == WSAEWOULDBLOCK) {
			target->type = TYPE_UNDEFINED;
			return;
		}
		closesocket(g_clientSocket);
		g_clientSocket = INVALID_SOCKET;
		goto back_here;
	} else if (bytes_received == 0) {
		closesocket(g_clientSocket);
		g_clientSocket = INVALID_SOCKET;
		goto back_here;
	} else {
		g_inBufferPos += bytes_received;
		if (g_inBufferPos >= MAX_MSG_LEN) {
			g_inBufferPos = MAX_MSG_LEN;
		}
		if (test_buffer_string(target)) {
			return;
		}
		if (g_inBufferPos >= MAX_MSG_LEN) {
			g_inBuffer[MAX_MSG_LEN] = '\0';
			gm_create_string(target, g_inBuffer);
			g_inBufferPos = 0;
			return;
		}
		if (blockingMode == 0) {
			goto read_stuff;
		}
		target->type = TYPE_UNDEFINED;
	}
}

void gm_api_network_has_client(struct gm_value *target, struct gm_instance *self, struct gm_instance *other, int paramCount, struct gm_value *parameters) {
	target->type = TYPE_INT32;
	if (g_serverSocket == INVALID_SOCKET) {
		target->value.i32 = 0;
		return;
	}
	if (test_network_connection() <= 0) {
		target->value.i32 = 0;
		return;
	}
	target->value.i32 = 1;
}

int make_internal_function(char *str, gm_function fn, int paramCount, bool flag) {
	for (int i = internal_function_count - 1; i >= 0; i--) {
		if (strcmp(internal_function_list[i].name, str) == 0) {
			return i;
		}
	}

	int index = internal_function_count;

	gm_create_internal_function(str, fn, paramCount, flag);

	return index;
}

gm_function get_internal_function(const char *name) {
	for (int i = internal_function_count - 1; i >= 0; i--) {
		if (strcmp(internal_function_list[i].name, name) == 0) {
			return internal_function_list[i].func;
		}
	}
	return 0;
}




/* ===================== INITIALIZATION FUNCTIONS ===================== */

void replace_function_references(int *internalFns, int count);

__declspec(dllexport) void init_extension() {
	int list[5] = {
		make_internal_function("network_create_text_server", &gm_api_network_create_text_server, 1, FALSE),
		make_internal_function("network_close_text_server", &gm_api_network_close_text_server, 0, FALSE),
		make_internal_function("network_send_text", &gm_api_network_send_text, 1, FALSE),
		make_internal_function("network_check_text", &gm_api_network_check_text, 0, FALSE),
		make_internal_function("network_has_client", &gm_api_network_has_client, 0, FALSE),
	};
	replace_function_references(list, 5);
}

// Replace occurrences of script functions with actual (internal) functions, don't use within currently used function
void replace_function_references(int *internalFns, int count) {
	if (script_count < 1) {
		// What
		return;
	}

	// Get bytecode of first script
	unsigned int *bytecode = script_list[0]->code->code_part->bytecode;

	// Trace back until invalid opcode
	while (1) {
		int instr = *bytecode;
		if ((instr & 0xFF000000) == 0) {
			bytecode--;
			if ((instr & 0x40000000) == 0) {
				bytecode--;
				if ((instr & 0x40000000) == 0) {
					bytecode += 3;
					break;
				}
			}
		}
		bytecode--;
	}

	// Iterate through all bytecode
	while (1) {
		unsigned int instr = *bytecode;
		unsigned int opcode = instr >> 24;
		if (opcode == 0 && instr != 0) {
			break;
		}
		if (opcode == 0xD9) {
			int functionId = *(bytecode + 1);
			if (functionId >= 100000) {
				functionId -= 100000;
				if (functionId < script_count) {
					char *name = script_names[functionId];
					for (int i = 0; i < count; i++) {
						if (strcmp(name, internal_function_list[internalFns[i]].name) == 0) {
							*(bytecode + 1) = internalFns[i];
							break;
						}
					}
				}
			}
		}
		if ((opcode & 0x40) != 0) {
			unsigned int type = (instr >> 16) & 0xF;
			if (type < 8) {
				bytecode++;
			}
			if (type == 0 || type == 3) {
				bytecode++;
			}
		}
		bytecode++;
	}
}

void gm_show_message(const char *msg) {
	gm_function fn = (gm_function) 0x516c00; // show_message

	struct gm_value target;
	struct gm_value strParam = {
		.value.string = &msg,
		.type = TYPE_STRING
	};
	(*fn)(&target, NULL, NULL, 1, &strParam);
}
