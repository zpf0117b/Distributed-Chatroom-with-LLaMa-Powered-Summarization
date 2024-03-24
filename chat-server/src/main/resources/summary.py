import json
from llamaapi import LlamaAPI

import sys

input_user = sys.argv[1].split('USRSEP')
input_msg = sys.argv[2].split('MSGSEP')
# Initialize the SDK
llama = LlamaAPI("YOUR_LLAMA_API_KEY")

msg = ""
for i in range(len(input_user)-1, -1, -1):
    msg += f"{input_user[i]}: {input_msg[i]}\n"

# print(msg)
# Build the API request
api_request_json = {
    "messages": [
        {"role": "assistant", "content": "Summarize the message between different users:\n" + msg},
    ],
    # "functions": [
    #     {
    #         "name": "get_current_weather",
    #         "description": "Get the current weather in a given location",
    #         "parameters": {
    #             "type": "object",
    #             "properties": {
    #                 "location": {
    #                     "type": "string",
    #                     "description": "The city and state, e.g. San Francisco, CA",
    #                 },
    #                 "days": {
    #                     "type": "number",
    #                     "description": "for how many days ahead you wants the forecast",
    #                 },
    #                 "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
    #             },
    #         },
    #         "required": ["location", "days"],
    #     }
    # ],
    "stream": False,
    # "function_call": "get_current_weather",
}

# Execute the Request
response = llama.run(api_request_json)
print(response.json().get('choices')[0].get('message').get('content'))
