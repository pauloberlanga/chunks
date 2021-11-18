import spacy
import os

def write_to_file(output_file_name, line) :
    file = open(output_file_name, 'a', encoding = 'utf-8')
    file.write(line + '\n')
    file.close()
    return

output_train_file_name = 'bosque_corpus/train.txt'
output_dev_file_name = 'bosque_corpus/dev.txt'
output_test_file_name = 'bosque_corpus/test.txt'

if os.path.exists(output_train_file_name) :
    os.remove(output_train_file_name)
if os.path.exists(output_dev_file_name) :
    os.remove(output_dev_file_name)
if os.path.exists(output_test_file_name) :
    os.remove(output_test_file_name)

spacy_nlp = spacy.load('pt_core_news_lg')

def pre_process(input_file_name, output_file_name) :

    prints = set()
    a, b = 0, 0
    with open(input_file_name, encoding = 'utf8') as file :
        id = ''
        annotation = ''
        source = ''
        for line in file :
            if line.startswith('CF') or line.startswith('CP') :
                id = line

            if line.startswith('A :') :
                annotation = line.replace('A : [', '').replace(']', '')
                annotation = ' '.join(annotation.split())
            if line.startswith('S :') :
                source = line.replace('S : [', '').replace(']', '').replace('%', '')
                source = ' '.join(source.split())

            if annotation and source :
                x, y, z = list(), list(), list()
                i = 0
                for token in spacy_nlp(source.strip()) :
                    x.append(token.text)
                    y.append(token.pos_)
                    try :
                        z.append(annotation.split()[i])
                    except IndexError as ie :
                        prints.add(id.split()[0].strip())
                        x, y, z = list(), list(), list()
                    i += 1

                last_is_o = True
                for j in range(len(x)) :
                    if (last_is_o == True and z[j] == 'X') :
                        label = 'B-NP'
                        last_is_o = False
                    elif (last_is_o == False and z[j] == 'X') :
                        label = 'I-NP'
                        last_is_o = False
                    else :
                        label = 'O'
                        last_is_o = True

                    write_to_file(output_file_name, x[j] + ' ' + y[j] + ' ' + label)

                if len(x) > 0 :
                    write_to_file(output_file_name, '')
                    a += 1
                else :
                    b += 1

                annotation = ''
                source = ''

    print('Foram processados com sucesso [' + str(a) + '] exemplos...')
    print('Foram descartados [' + str(b) + '] exemplos: [' + ', '.join(str(p) for p in prints) + ']')
    print('')

# ----------------------------------------------------------------------------------------------------------------------

input_train_file_name = 'C:/Users/Paulo.Berlanga/git-nlp/chunks/nlp-np/src/test/txt/train_annotated.txt'
input_dev_file_name = 'C:/Users/Paulo.Berlanga/git-nlp/chunks/nlp-np/src/test/txt/dev_annotated.txt'
input_test_file_name = 'C:/Users/Paulo.Berlanga/git-nlp/chunks/nlp-np/src/test/txt/test_annotated.txt'

print('')
print('Processando conjunto de treinamento...')
pre_process(input_train_file_name, output_train_file_name)
print('Processando conjunto de validação...')
pre_process(input_dev_file_name, output_dev_file_name)
print('Processando conjunto de teste...')
pre_process(input_test_file_name, output_test_file_name)
