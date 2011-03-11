#!/bin/bash
#
# The list of crawl commands that we used.  Expect this to take a long
# time to run. What we did in practice was to kill the recursive wget
# commands after about 4 hours of execution.

wget -r -l inf -D hillary4president.org hillary4president.org -R png,jpg,jpeg,js,css,gif,pdf -olog-hillary&
wget -r -l inf -D freestrongamerica.com freestrongamerica.com -R png,jpg,jpeg,js,css,gif,pdf -olog-mittromney&
wget -r -l inf -D chrisdodd.com chrisdodd.com -R png,jpg,jpeg,js,css,gif,pdf -olog-chrisdodd&
wget -r -l inf -D kucinich.house.gov kucinich.house.gov -R png,jpg,jpeg,js,css,gif,pdf -olog-kucinich&
wget -r -l inf -D www.jerrybrown.org www.jerrybrown.org -R png,jpg,jpeg,js,css,gif,pdf -olog-jerrybrown&
wget -r -l inf -D www.johnmccain.com www.johnmccain.com -R png,jpg,jpeg,js,css,gif,pdf -olog-mccain&
wget -r -l inf -D www.ronpaulforcongress.com www.ronpaulforcongress.com -R png,jpg,jpeg,js,css,gif,pdf -olog-ronpaul&
wget -r -l inf -D www.4biden.com www.4biden.com -R png,jpg,jpeg,js,css,gif,pdf -olog-biden&
wget -r -l inf -D www.washingtonpost.com www.washingtonpost.com -R png,jpg,jpeg,js,css,gif,pdf -olog-washingtonpost.com&
wget -r -l inf -D www.msnbc.msn.com www.msnbc.msn.com -R png,jpg,jpeg,js,css,gif,pdf -olog-msn&
wget -r -l inf -D www.bbc.co.uk www.bbc.co.uk -R png,jpg,jpeg,js,css,gif,pdf -olog-bbc&
wget -r -l inf -D www.nytimes.com www.nytimes.com -R png,jpg,jpeg,js,css,gif,pdf -olog-nytimes&
wget -r -l inf -D www.huffingtonpost.com www.huffingtonpost.com -R png,jpg,jpeg,js,css,gif,pdf -olog-huffingtonpost&
wget -r -l inf -D www.cnn.com www.cnn.com -R png,jpg,jpeg,js,css,gif,pdf -olog-cnn&
wget -r -l inf -D www.latimes.com www.latimes.com -R png,jpg,jpeg,js,css,gif,pdf -olog-latimes&
wget -r -l inf -D foxnews.com www.foxnews.com -X on-air -R png,jpg,jpeg,js,css,gif,pdf -olog-foxnews&
wget -r -l inf -D www.barackobama.com,my.barackobama.com www.barackobama.com -R png,jpg,jpeg,js,css,gif,pdf -olog-obama&

wait
